from pydantic import BaseModel
from typing import List, Optional


# ============================================
# 공통 Input
# ============================================

class TextInput(BaseModel):
    text: str

    model_config = {
        "json_schema_extra": {
            "example": {
                "text": "여기에 자기소개서를 입력하세요."
            }
        }
    }


# ============================================
# 이력서 저장 (구직자 이력서 → Redis Vector)
# ============================================

class ResumeInput(BaseModel):
    candidate_id: str
    resume_text: str

    model_config = {
        "json_schema_extra": {
            "example": {
                "candidate_id": "1",
                "resume_text": "5년차 백엔드 개발자. Java, Spring Boot 전문…"
            }
        }
    }


class SaveResumeResponse(BaseModel):
    status: str
    candidate_id: str


# ============================================
# 채용공고 저장 (채용공고 → Redis Vector)
# ============================================

class JobpostingInput(BaseModel):
    jobposting_id: str
    jobposting_text: str

    model_config = {
        "json_schema_extra": {
            "example": {
                "jobposting_id": "100",
                "jobposting_text": "백엔드 개발자 채용. Java, Spring Boot, JPA 필수. Kafka, Kubernetes 우대."
            }
        }
    }


class SaveJobpostingResponse(BaseModel):
    status: str
    jobposting_id: str


# ============================================
# 후보자 매칭 (회사 → 후보자 검색)
# ============================================

class MatchCandidatesRequest(BaseModel):
    jd_text: str
    required_skills: Optional[List[str]] = None
    top_k: int = 5

    model_config = {
        "json_schema_extra": {
            "example": {
                "jd_text": "Spring Boot 경력 3년 이상, Redis, Kafka 경험자 우대",
                "top_k": 5
            }
        }
    }


class MatchItem(BaseModel):
    candidate_id: str
    score: float


class MatchCandidatesResponse(BaseModel):
    matches: List[MatchItem]


# ============================================
# 채용공고 매칭 (구직자 → 맞는 공고 검색)
# ============================================

class MatchJobpostingsRequest(BaseModel):
    resume_text: str
    top_k: int = 5

    model_config = {
        "json_schema_extra": {
            "example": {
                "resume_text": "Java, Spring Boot, JPA, Redis 경험 백엔드 개발자",
                "top_k": 5
            }
        }
    }


class JobpostingMatchItem(BaseModel):
    jobposting_id: str
    score: float


class MatchJobpostingsResponse(BaseModel):
    matches: List[JobpostingMatchItem]


# ============================================
# 스코어링
# ============================================

class ScoreRequest(BaseModel):
    jd_text: str
    candidate_id: str
    required_skills: Optional[List[str]] = None

    model_config = {
        "json_schema_extra": {
            "example": {
                "candidate_id": "1",
                "jd_text": "Spring Boot와 Docker 경험 필수",
                "required_skills": ["Spring Boot", "Docker"]
            }
        }
    }


class ScoreDetail(BaseModel):
    skill_ratio: float
    skill_score: float
    similarity_score: float
    bonus_score: float
    total_score: float
    grade: str


class ScoreResponse(BaseModel):
    candidate_id: str
    required_skills: List[str]
    candidate_skills: List[str]
    cosine_similarity: float
    score_detail: ScoreDetail


# ============================================
# 스킬 추출
# ============================================

class SkillsResponse(BaseModel):
    skills: List[str]


# ============================================
# 스킬 갭 분석 (구직자용)
# ============================================

class SkillGapRequest(BaseModel):
    candidate_id: str
    jobposting_id: str

    model_config = {
        "json_schema_extra": {
            "example": {
                "candidate_id": "1",
                "jobposting_id": "100"
            }
        }
    }


class SkillGapResponse(BaseModel):
    candidate_id: str
    jobposting_id: str
    candidate_skills: List[str]
    required_skills: List[str]
    matched_skills: List[str]
    missing_skills: List[str]
    match_rate: float
    cosine_similarity: float


# ============================================
# 요약 (유지)
# ============================================

class SummaryResponse(BaseModel):
    summary: str
