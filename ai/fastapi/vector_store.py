import os
import numpy as np
import redis

from redis.commands.search.field import VectorField, TextField
from redis.commands.search.indexDefinition import IndexDefinition, IndexType
from redis.commands.search.query import Query

REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))
VECTOR_DIM = int(os.getenv("VECTOR_DIM", "768"))

# 이력서 인덱스
RESUME_INDEX = "resume_index"
RESUME_PREFIX = "candidate:"

# 채용공고 인덱스
JOBPOSTING_INDEX = "jobposting_index"
JOBPOSTING_PREFIX = "jobposting:"

r = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, decode_responses=False)


# ============================================
# 인덱스 생성
# ============================================

def _create_vector_index(index_name: str, prefix: str):
    """벡터 인덱스 생성 (공통)"""
    try:
        r.ft(index_name).info()
        print(f"[Redis] Index '{index_name}' exists")
    except Exception:
        schema = [
            VectorField(
                "embedding",
                "HNSW",
                {
                    "TYPE": "FLOAT32",
                    "DIM": VECTOR_DIM,
                    "DISTANCE_METRIC": "COSINE",
                    "M": 16,
                    "EF_CONSTRUCTION": 200,
                },
            ),
        ]
        definition = IndexDefinition(prefix=[prefix], index_type=IndexType.HASH)
        r.ft(index_name).create_index(schema, definition=definition)
        print(f"[Redis] Index '{index_name}' created")


def create_index():
    """이력서 + 채용공고 인덱스 모두 생성"""
    _create_vector_index(RESUME_INDEX, RESUME_PREFIX)
    _create_vector_index(JOBPOSTING_INDEX, JOBPOSTING_PREFIX)


# ============================================
# 이력서 저장/조회/검색
# ============================================

def save_resume(candidate_id: str, embedding: list[float], resume_text: str) -> None:
    key = f"{RESUME_PREFIX}{candidate_id}"
    vec = np.asarray(embedding, dtype=np.float32).tobytes()
    r.hset(key, mapping={
        "embedding": vec,
        "resume_text": resume_text.encode("utf-8"),
    })


def get_resume(candidate_id: str):
    key = f"{RESUME_PREFIX}{candidate_id}"
    data = r.hgetall(key)
    if not data:
        return None
    emb_bytes = data.get(b"embedding")
    txt_bytes = data.get(b"resume_text")
    emb = None
    if emb_bytes:
        emb = np.frombuffer(emb_bytes, dtype=np.float32)
    resume_text = txt_bytes.decode("utf-8") if txt_bytes else ""
    return {"embedding": emb, "resume_text": resume_text}


def search_similar_resumes(embedding: list[float], k: int = 5):
    """JD 임베딩으로 유사한 이력서 검색 (회사용)"""
    return _vector_search(RESUME_INDEX, embedding, k)


# ============================================
# 채용공고 저장/조회/검색
# ============================================

def save_jobposting(jobposting_id: str, embedding: list[float], jobposting_text: str) -> None:
    key = f"{JOBPOSTING_PREFIX}{jobposting_id}"
    vec = np.asarray(embedding, dtype=np.float32).tobytes()
    r.hset(key, mapping={
        "embedding": vec,
        "jobposting_text": jobposting_text.encode("utf-8"),
    })


def get_jobposting(jobposting_id: str):
    key = f"{JOBPOSTING_PREFIX}{jobposting_id}"
    data = r.hgetall(key)
    if not data:
        return None
    emb_bytes = data.get(b"embedding")
    txt_bytes = data.get(b"jobposting_text")
    emb = None
    if emb_bytes:
        emb = np.frombuffer(emb_bytes, dtype=np.float32)
    jobposting_text = txt_bytes.decode("utf-8") if txt_bytes else ""
    return {"embedding": emb, "jobposting_text": jobposting_text}


def search_similar_jobpostings(embedding: list[float], k: int = 5):
    """이력서 임베딩으로 유사한 채용공고 검색 (구직자용)"""
    return _vector_search(JOBPOSTING_INDEX, embedding, k)


# ============================================
# 공통 벡터 검색
# ============================================

def _vector_search(index_name: str, embedding: list[float], k: int = 5):
    vec = np.asarray(embedding, dtype=np.float32).tobytes()
    q = (
        Query(f"*=>[KNN {k} @embedding $vec AS score]")
        .return_fields("score")
        .sort_by("score")
        .dialect(2)
    )
    res = r.ft(index_name).search(q, query_params={"vec": vec})

    out = []
    for doc in res.docs:
        out.append({
            "key": doc.id,
            "score": float(getattr(doc, "score", 0.0)),
        })
    return out
