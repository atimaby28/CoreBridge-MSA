import os
import time
import numpy as np
from fastapi import FastAPI, HTTPException
from fastapi.responses import Response
from dotenv import load_dotenv

from prometheus_client import (
    Counter, Histogram, Gauge, generate_latest, REGISTRY
)

# ---- 기존 Import ----
from models import TextInput, ResumeInput, MatchRequest, ScoreRequest, SummaryResponse, SkillsResponse, MatchResponse, SaveResumeResponse, ScoreResponse
from vector_store import create_index, save_resume, search_similar, get_resume
from llm import summarize, extract_skills
from scoring import rule_score

import ollama
from openai import OpenAI
import redis

load_dotenv()

tags_metadata = [
    {
        "name": "Summary",
        "description": "이력서/텍스트 요약을 수행하는 엔드포인트입니다.",
    },
    {
        "name": "Skills",
        "description": "텍스트에서 기술 스택을 추출하는 엔드포인트입니다.",
    },
    {
        "name": "Matching",
        "description": "JD 텍스트와 저장된 이력서를 벡터 기반으로 매칭합니다.",
    },
    {
        "name": "Scoring",
        "description": "JD와 후보자의 이력서를 기반으로 점수와 상세 평가를 계산합니다.",
    },
    {
        "name": "Monitoring",
        "description": "Prometheus용 메트릭 엔드포인트입니다. (Swagger에는 숨김 처리)",
    },
]

app = FastAPI(
    title="CoreBridge AI Matching Service",
    description="Ollama + Redis + Vector Store 기반의 이력서-JD 매칭/스코어링 서비스",
    version="1.0.0",
    openapi_tags=tags_metadata,
)

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
# 🔥 기존 메트릭
# ============================================

REQUEST_COUNT = Counter(
    "ai_service_requests_total",
    "Total number of requests per endpoint",
    ["endpoint"]
)

REQUEST_LATENCY = Histogram(
    "ai_service_request_latency_seconds",
    "Latency of requests in seconds",
    ["endpoint"]
)

OLLAMA_LATENCY = Gauge(
    "ai_service_ollama_latency_ms",
    "Latency of Ollama processing"
)

EMBEDDING_LATENCY = Gauge(
    "ai_service_embedding_latency_ms",
    "Latency of embedding generation"
)

# ============================================
# 🔥 추가되는 파이프라인 메트릭
# ============================================

SUMMARY_LAT = Gauge("ai_service_summary_latency_ms", "Summary latency")
SKILLS_LAT = Gauge("ai_service_skills_latency_ms", "Skills latency")
MATCH_LAT = Gauge("ai_service_match_latency_ms", "Match latency")
SCORE_LAT = Gauge("ai_service_score_latency_ms", "Score latency")

REDIS_LAT = Gauge("ai_service_redis_latency_ms", "Redis latency")

WORKFLOW_TOTAL = Gauge(
    "ai_workflow_total_processing_ms",
    "Entire n8n workflow total processing time"
)

ERROR_COUNT = Counter(
    "ai_service_errors_total",
    "Total errors in AI service",
    ["endpoint"]
)

# ============================================
# Utility: Embedding
# ============================================

def embed(text: str):
    start = time.time()
    try:
        if EMBEDDING_BACKEND == "ollama":
            resp = ollama.embeddings(model=EMBEDDING_MODEL, prompt=text)
            emb = resp["embedding"]
        else:
            resp = openai_client.embeddings.create(
                model=EMBEDDING_MODEL, input=text
            )
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
    denom = (np.linalg.norm(a) * np.linalg.norm(b))
    if denom == 0:
        return 0.0
    return float(np.dot(a, b) / denom)

create_index()

# ============================================
# 🔥 Redis latency 측정기
# ============================================

def measure_redis_latency():
    t0 = time.time()
    redis_client.ping()
    REDIS_LAT.set((time.time() - t0) * 1000)

# ============================================
# 🔥 Endpoints
# ============================================

@app.post(
    "/summary",
    response_model=SummaryResponse,
    tags=["Summary"],
    summary="텍스트 요약",
    description="입력 텍스트를 LLM(Ollama)을 사용해 한글 요약으로 변환합니다.",
)
def api_summary(req: TextInput):
    endpoint = "/summary"
    REQUEST_COUNT.labels(endpoint).inc()
    measure_redis_latency()

    with REQUEST_LATENCY.labels(endpoint).time():
        t0 = time.time()
        try:
            result = summarize(req.text)
            lat = (time.time() - t0) * 1000
            SUMMARY_LAT.set(lat)
            OLLAMA_LATENCY.set(lat)
            return {"summary": result}
        except:
            ERROR_COUNT.labels(endpoint=endpoint).inc()
            raise


@app.post(
    "/skills",
    response_model=SkillsResponse,
    tags=["Skills"],
    summary="기술 스택 추출",
    description="텍스트(이력서, JD 등)에서 기술 스택/키워드를 추출합니다.",
)
def api_skills(req: TextInput):
    endpoint = "/skills"
    REQUEST_COUNT.labels(endpoint).inc()
    measure_redis_latency()

    with REQUEST_LATENCY.labels(endpoint).time():
        t0 = time.time()
        try:
            result = extract_skills(req.text)
            lat = (time.time() - t0) * 1000
            SKILLS_LAT.set(lat)
            OLLAMA_LATENCY.set(lat)
            return {"skills": result}
        except:
            ERROR_COUNT.labels(endpoint=endpoint).inc()
            raise


@app.post(
    "/match_jd",
    response_model=MatchResponse,
    tags=["Matching"],
)
def api_match(req: MatchRequest):
    endpoint = "/match_jd"
    REQUEST_COUNT.labels(endpoint).inc()
    measure_redis_latency()

    with REQUEST_LATENCY.labels(endpoint).time():
        t0 = time.time()
        try:
            jd_emb = embed(req.jd_text)
            hits = search_similar(jd_emb, k=req.top_k)

            # ---- 🔥 여기서 필드 변환 ----
            formatted = []
            for h in hits:
                formatted.append({
                    "candidate_id": h["key"].replace("candidate:", ""),
                    "score": h["score"]
                })

            MATCH_LAT.set((time.time() - t0) * 1000)

            return {"matches": formatted}

        except:
            ERROR_COUNT.labels(endpoint=endpoint).inc()
            raise


@app.post(
    "/save_resume",
    response_model=SaveResumeResponse,
    tags=["Matching"],
    summary="이력서 벡터 저장",
    description="후보자의 이력서를 임베딩 후 벡터 스토어에 저장합니다.",
)
def api_save_resume(req: ResumeInput):
    endpoint = "/save_resume"
    REQUEST_COUNT.labels(endpoint).inc()
    measure_redis_latency()

    with REQUEST_LATENCY.labels(endpoint).time():
        try:
            # embedding 처리
            t0 = time.time()
            emb = embed(req.resume_text)
            lat = (time.time() - t0) * 1000

            # 임베딩 latency는 embed() 내부에서 이미 기록됨
            # 별도 latency 메트릭을 만들지 않아도 됨

            save_resume(req.candidate_id, emb, req.resume_text)

            return {
                "status": "saved",
                "candidate_id": req.candidate_id
            }

        except Exception:
            ERROR_COUNT.labels(endpoint=endpoint).inc()
            raise

@app.post(
    "/score",
    response_model=ScoreResponse,
    tags=["Scoring"],
    summary="후보자 점수 계산",
    description="JD와 후보자 이력서를 기준으로 유사도/스킬 매칭을 계산하고 상세 점수를 반환합니다.",
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
                "score_detail": detail
            }
        except:
            ERROR_COUNT.labels(endpoint=endpoint).inc()
            raise


# ============================================
# 🔥 metrics endpoint
# ============================================

@app.get(
    "/metrics",
    include_in_schema=False,     # ⬅ Swagger(/docs)에서 숨김
)
def metrics():
    return Response(generate_latest(REGISTRY), media_type="text/plain")