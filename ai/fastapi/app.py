import os
import time
import numpy as np
from fastapi import FastAPI, HTTPException
from fastapi.responses import Response
from dotenv import load_dotenv

from prometheus_client import (
    Counter, Histogram, Gauge, generate_latest, REGISTRY
)

from models import (
    TextInput, ResumeInput, JobpostingInput,
    MatchCandidatesRequest, MatchJobpostingsRequest,
    ScoreRequest, SkillGapRequest,
    SummaryResponse, SkillsResponse,
    MatchCandidatesResponse, MatchJobpostingsResponse,
    SaveResumeResponse, SaveJobpostingResponse,
    ScoreResponse, SkillGapResponse,
)
from vector_store import (
    create_index,
    save_resume, get_resume, search_similar_resumes,
    save_jobposting, get_jobposting, search_similar_jobpostings,
)
from llm import summarize, extract_skills
from scoring import rule_score

import ollama
from openai import OpenAI
import redis

load_dotenv()

# ============================================
# FastAPI App
# ============================================

tags_metadata = [
    {"name": "Resume", "description": "이력서 벡터 저장 및 조회"},
    {"name": "Jobposting", "description": "채용공고 벡터 저장 및 조회"},
    {"name": "Matching", "description": "양방향 매칭 (후보자↔채용공고)"},
    {"name": "Scoring", "description": "후보자-채용공고 상세 점수 산출"},
    {"name": "Skills", "description": "텍스트에서 기술 스택 추출"},
    {"name": "Analysis", "description": "스킬 갭 분석"},
    {"name": "Summary", "description": "텍스트 요약"},
]

app = FastAPI(
    title="CoreBridge AI Matching Service",
    description="Ollama + Redis Vector 기반 양방향 이력서-채용공고 매칭/스코어링 서비스",
    version="2.0.0",
    openapi_tags=tags_metadata,
)

# ============================================
# Config
# ============================================

GEN_MODEL = os.getenv("GEN_MODEL", "llama3")
EMBEDDING_BACKEND = os.getenv("EMBEDDING_BACKEND", "ollama")
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "nomic-embed-text")

REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))

redis_client = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, db=0)

openai_client = None
if EMBEDDING_BACKEND == "openai":
    openai_client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

# ============================================
# Prometheus Metrics
# ============================================

REQUEST_COUNT = Counter("ai_service_requests_total", "Total requests per endpoint", ["endpoint"])
REQUEST_LATENCY = Histogram("ai_service_request_latency_seconds", "Latency per endpoint", ["endpoint"])
OLLAMA_LATENCY = Gauge("ai_service_ollama_latency_ms", "Ollama processing latency")
EMBEDDING_LATENCY = Gauge("ai_service_embedding_latency_ms", "Embedding generation latency")
MATCH_LAT = Gauge("ai_service_match_latency_ms", "Match latency")
SCORE_LAT = Gauge("ai_service_score_latency_ms", "Score latency")
REDIS_LAT = Gauge("ai_service_redis_latency_ms", "Redis latency")
ERROR_COUNT = Counter("ai_service_errors_total", "Total errors", ["endpoint"])

# ============================================
# Utility
# ============================================

def embed(text: str):
    start = time.time()
    try:
        if EMBEDDING_BACKEND == "ollama":
            resp = ollama.embeddings(model=EMBEDDING_MODEL, prompt=text)
            emb = resp["embedding"]
        else:
            resp = openai_client.embeddings.create(model=EMBEDDING_MODEL, input=text)
            emb = resp.data[0].embedding
        EMBEDDING_LATENCY.set((time.time() - start) * 1000)
        return emb
    except:
        ERROR_COUNT.labels(endpoint="embedding").inc()
        raise


def cosine(a: np.ndarray, b: np.ndarray) -> float:
    if a is None or b is None:
        return 0.0
    if a.ndim != 1:
        a = a.ravel()
    if b.ndim != 1:
        b = b.ravel()
    denom = np.linalg.norm(a) * np.linalg.norm(b)
    if denom == 0:
        return 0.0
    return float(np.dot(a, b) / denom)


def measure_redis_latency():
    t0 = time.time()
    redis_client.ping()
    REDIS_LAT.set((time.time() - t0) * 1000)


# 인덱스 생성
create_index()


# ============================================
# 이력서 저장 (구직자)
# ============================================

@app.post(
    "/save_resume",
    response_model=SaveResumeResponse,
    tags=["Resume"],
    summary="이력서 벡터 저장",
    description="구직자의 이력서를 임베딩 후 벡터 스토어에 저장합니다.",
)
def api_save_resume(req: ResumeInput):
    endpoint = "/save_resume"
    REQUEST_COUNT.labels(endpoint).inc()
    measure_redis_latency()

    with REQUEST_LATENCY.labels(endpoint).time():
        try:
            emb = embed(req.resume_text)
            save_resume(req.candidate_id, emb, req.resume_text)
            return {"status": "saved", "candidate_id": req.candidate_id}
        except Exception:
            ERROR_COUNT.labels(endpoint=endpoint).inc()
            raise


# ============================================
# 채용공고 저장 (회사)
# ============================================

@app.post(
    "/save_jobposting",
    response_model=SaveJobpostingResponse,
    tags=["Jobposting"],
    summary="채용공고 벡터 저장",
    description="채용공고를 임베딩 후 벡터 스토어에 저장합니다.",
)
def api_save_jobposting(req: JobpostingInput):
    endpoint = "/save_jobposting"
    REQUEST_COUNT.labels(endpoint).inc()
    measure_redis_latency()

    with REQUEST_LATENCY.labels(endpoint).time():
        try:
            emb = embed(req.jobposting_text)
            save_jobposting(req.jobposting_id, emb, req.jobposting_text)
            return {"status": "saved", "jobposting_id": req.jobposting_id}
        except Exception:
            ERROR_COUNT.labels(endpoint=endpoint).inc()
            raise


# ============================================
# 후보자 매칭 (회사 → 후보자 검색)
# ============================================

@app.post(
    "/match_candidates",
    response_model=MatchCandidatesResponse,
    tags=["Matching"],
    summary="채용공고 기반 후보자 매칭",
    description="채용공고 내용으로 유사한 이력서를 가진 후보자를 검색합니다.",
)
def api_match_candidates(req: MatchCandidatesRequest):
    endpoint = "/match_candidates"
    REQUEST_COUNT.labels(endpoint).inc()
    measure_redis_latency()

    with REQUEST_LATENCY.labels(endpoint).time():
        t0 = time.time()
        try:
            jd_emb = embed(req.jd_text)
            hits = search_similar_resumes(jd_emb, k=req.top_k)

            formatted = []
            for h in hits:
                formatted.append({
                    "candidate_id": h["key"].replace("candidate:", ""),
                    "score": h["score"],
                })

            MATCH_LAT.set((time.time() - t0) * 1000)
            return {"matches": formatted}
        except:
            ERROR_COUNT.labels(endpoint=endpoint).inc()
            raise


# ============================================
# 기존 match_jd 호환 (회사 화면에서 사용 중)
# ============================================

@app.post(
    "/match_jd",
    response_model=MatchCandidatesResponse,
    tags=["Matching"],
    summary="[호환] 채용공고 기반 후보자 매칭",
    description="match_candidates와 동일. 기존 호환용.",
)
def api_match_jd(req: MatchCandidatesRequest):
    return api_match_candidates(req)


# ============================================
# 채용공고 매칭 (구직자 → 맞는 공고 검색)
# ============================================

@app.post(
    "/match_jobpostings",
    response_model=MatchJobpostingsResponse,
    tags=["Matching"],
    summary="이력서 기반 채용공고 추천",
    description="구직자의 이력서로 유사한 채용공고를 검색합니다.",
)
def api_match_jobpostings(req: MatchJobpostingsRequest):
    endpoint = "/match_jobpostings"
    REQUEST_COUNT.labels(endpoint).inc()
    measure_redis_latency()

    with REQUEST_LATENCY.labels(endpoint).time():
        t0 = time.time()
        try:
            resume_emb = embed(req.resume_text)
            hits = search_similar_jobpostings(resume_emb, k=req.top_k)

            formatted = []
            for h in hits:
                formatted.append({
                    "jobposting_id": h["key"].replace("jobposting:", ""),
                    "score": h["score"],
                })

            MATCH_LAT.set((time.time() - t0) * 1000)
            return {"matches": formatted}
        except:
            ERROR_COUNT.labels(endpoint=endpoint).inc()
            raise


# ============================================
# 스코어링 (회사용 상세 점수)
# ============================================

@app.post(
    "/score",
    response_model=ScoreResponse,
    tags=["Scoring"],
    summary="후보자 상세 점수 계산",
    description="JD와 후보자 이력서를 기준으로 상세 점수를 반환합니다.",
)
def api_score(req: ScoreRequest):
    endpoint = "/score"
    REQUEST_COUNT.labels(endpoint).inc()
    measure_redis_latency()

    with REQUEST_LATENCY.labels(endpoint).time():
        t0 = time.time()
        try:
            cand = get_resume(req.candidate_id)
            if not cand:
                raise HTTPException(404, "candidate not found")

            cand_emb = cand["embedding"]
            cand_text = cand["resume_text"]

            jd_emb = embed(req.jd_text)
            cos = cosine(np.array(jd_emb, dtype=np.float32), cand_emb)

            jd_skills = req.required_skills or extract_skills(req.jd_text)
            cand_skills = extract_skills(cand_text)

            detail = rule_score(jd_skills, cand_skills, cos)

            SCORE_LAT.set((time.time() - t0) * 1000)
            return {
                "candidate_id": req.candidate_id,
                "required_skills": jd_skills,
                "candidate_skills": cand_skills,
                "cosine_similarity": round(cos, 4),
                "score_detail": detail,
            }
        except HTTPException:
            raise
        except:
            ERROR_COUNT.labels(endpoint=endpoint).inc()
            raise


# ============================================
# 스킬 갭 분석 (구직자용)
# ============================================

@app.post(
    "/skill_gap",
    response_model=SkillGapResponse,
    tags=["Analysis"],
    summary="스킬 갭 분석",
    description="구직자의 보유 스킬과 채용공고 요구 스킬을 비교하여 부족한 스킬을 분석합니다.",
)
def api_skill_gap(req: SkillGapRequest):
    endpoint = "/skill_gap"
    REQUEST_COUNT.labels(endpoint).inc()
    measure_redis_latency()

    with REQUEST_LATENCY.labels(endpoint).time():
        try:
            cand = get_resume(req.candidate_id)
            if not cand:
                raise HTTPException(404, "candidate not found")

            jp = get_jobposting(req.jobposting_id)
            if not jp:
                raise HTTPException(404, "jobposting not found")

            cand_skills = extract_skills(cand["resume_text"])
            req_skills = extract_skills(jp["jobposting_text"])

            cand_set = set(s.lower() for s in cand_skills)
            req_set = set(s.lower() for s in req_skills)

            matched = sorted(cand_set & req_set)
            missing = sorted(req_set - cand_set)
            match_rate = len(matched) / max(1, len(req_set))

            # 코사인 유사도
            cos = cosine(
                np.array(cand["embedding"], dtype=np.float32) if cand["embedding"] is not None else np.zeros(1),
                np.array(jp["embedding"], dtype=np.float32) if jp["embedding"] is not None else np.zeros(1),
            )

            return {
                "candidate_id": req.candidate_id,
                "jobposting_id": req.jobposting_id,
                "candidate_skills": cand_skills,
                "required_skills": req_skills,
                "matched_skills": matched,
                "missing_skills": missing,
                "match_rate": round(match_rate, 3),
                "cosine_similarity": round(cos, 4),
            }
        except HTTPException:
            raise
        except:
            ERROR_COUNT.labels(endpoint=endpoint).inc()
            raise


# ============================================
# 스킬 추출
# ============================================

@app.post(
    "/skills",
    response_model=SkillsResponse,
    tags=["Skills"],
    summary="기술 스택 추출",
    description="텍스트에서 기술 스택/키워드를 추출합니다.",
)
def api_skills(req: TextInput):
    endpoint = "/skills"
    REQUEST_COUNT.labels(endpoint).inc()
    measure_redis_latency()

    with REQUEST_LATENCY.labels(endpoint).time():
        try:
            result = extract_skills(req.text)
            OLLAMA_LATENCY.set(0)
            return {"skills": result}
        except:
            ERROR_COUNT.labels(endpoint=endpoint).inc()
            raise


# ============================================
# 요약
# ============================================

@app.post(
    "/summary",
    response_model=SummaryResponse,
    tags=["Summary"],
    summary="텍스트 요약",
    description="입력 텍스트를 LLM을 사용해 한글 요약으로 변환합니다.",
)
def api_summary(req: TextInput):
    endpoint = "/summary"
    REQUEST_COUNT.labels(endpoint).inc()
    measure_redis_latency()

    with REQUEST_LATENCY.labels(endpoint).time():
        t0 = time.time()
        try:
            result = summarize(req.text)
            OLLAMA_LATENCY.set((time.time() - t0) * 1000)
            return {"summary": result}
        except:
            ERROR_COUNT.labels(endpoint=endpoint).inc()
            raise


# ============================================
# Metrics (Prometheus)
# ============================================

@app.get("/metrics", include_in_schema=False)
def metrics():
    return Response(generate_latest(REGISTRY), media_type="text/plain")
