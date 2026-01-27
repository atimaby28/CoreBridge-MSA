from pydantic import BaseModel
from typing import List, Optional

class TextInput(BaseModel):
    text: str

    model_config = {
        "json_schema_extra": {
            "example": {
                "text": "여기에 자기소개서를 입력하세요."
            }
        }
    }

class ResumeInput(BaseModel):
    candidate_id: str
    resume_text: str

    model_config = {
        "json_schema_extra": {
            "example": {
                "candidate_id": "1",
                "resume_text": "양승우의 0년 경력 백엔드 개발자 이력서 텍스트…"
            }
        }
    }

class MatchRequest(BaseModel):
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

class ScoreRequest(BaseModel):
    jd_text: str
    candidate_id: str
    required_skills: Optional[List[str]] = None

    model_config = {
        "json_schema_extra": {
            "example": {
                "candidate_id": "1",
                "jd_text": "Spring Boot와 Docker 경험 필수",
                "required_skills": ["spring", "docker"]
            }
        }
    }

class SummaryResponse(BaseModel):
    summary: str

    model_config = {
        "json_schema_extra": {
            "example": {
                "summary": "이 지원자는 Java/Spring 기반의 백엔드 개발 경험을 보유하고 있으며..."
            }
        }
    }


class SkillsResponse(BaseModel):
    skills: List[str]

    model_config = {
        "json_schema_extra": {
            "example": {
                "skills": ["python", "fastapi", "docker", "aws"]
            }
        }
    }

class MatchItem(BaseModel):
    candidate_id: str
    score: float

class MatchResponse(BaseModel):
    matches: List[MatchItem]

    model_config = {
        "json_schema_extra": {
            "example": {
                "matches": [
                    {
                        "candidate_id": "1",
                        "score": 0.87,
                        "resume_text": "백엔드 개발자로서 Spring Boot 기반..."
                    },
                    {
                        "candidate_id": "2",
                        "score": 0.81,
                        "resume_text": "5년차 Java 개발자로서 REST API 개발 경험..."
                    }
                ]
            }
        }
    }

class SaveResumeResponse(BaseModel):
    status: str
    candidate_id: str

    model_config = {
        "json_schema_extra": {
            "example": {
                "status": "saved",
                "candidate_id": "1"
            }
        }
    }

class ScoreDetail(BaseModel):
    skill_ratio: float
    skill_score: float
    sim_score: float
    bonus: float
    total: float

    model_config = {
        "json_schema_extra": {
            "example": {
                "skill_ratio": 1.0,
                "skill_score": 60.0,
                "sim_score": 20.0,
                "bonus": 10.0,
                "total": 90.0
            }
        }
    }

class ScoreResponse(BaseModel):
    candidate_id: str
    required_skills: List[str]
    candidate_skills: List[str]
    cosine_similarity: float
    score_detail: ScoreDetail

    model_config = {
        "json_schema_extra": {
            "example": {
                "candidate_id": "123",
                "required_skills": ["spring", "docker"],
                "candidate_skills": ["spring", "docker", "redis"],
                "cosine_similarity": 0.82,
                "score_detail": {
                    "skill_ratio": 1.0,
                    "skill_score": 60.0,
                    "sim_score": 20.0,
                    "bonus": 10.0,
                    "total": 90.0
                }
            }
        }
    }
