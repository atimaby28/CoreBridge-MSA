import os
import numpy as np
import redis

from redis.commands.search.field import VectorField
from redis.commands.search.indexDefinition import IndexDefinition, IndexType
from redis.commands.search.query import Query

REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))
VECTOR_DIM = int(os.getenv("VECTOR_DIM", "768"))
INDEX_NAME = "resume_index"
PREFIX = "candidate:"

r = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, decode_responses=False)

def create_index():
    try:
        r.ft(INDEX_NAME).info()
        print("[Redis] Index exists")
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
        definition = IndexDefinition(prefix=[PREFIX], index_type=IndexType.HASH)
        r.ft(INDEX_NAME).create_index(schema, definition=definition)
        print("[Redis] Index created")

def key_of(candidate_id: str) -> str:
    return f"{PREFIX}{candidate_id}"

def save_resume(candidate_id: str, embedding: list[float], resume_text: str) -> None:
    key = key_of(candidate_id)
    vec = np.asarray(embedding, dtype=np.float32).tobytes()
    r.hset(key, mapping={"embedding": vec, "resume_text": resume_text.encode("utf-8")})

def get_resume(candidate_id: str):
    key = key_of(candidate_id)
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

def search_similar(embedding: list[float], k: int = 5):
    vec = np.asarray(embedding, dtype=np.float32).tobytes()
    # "__score" 정렬 및 반환 필드 명시
    q = (
        Query(f"*=>[KNN {k} @embedding $vec AS score]")
        .return_fields("candidate_id", "score")
        .sort_by("score")
        .dialect(2)
    )

    # Redis 쿼리 실행
    res = r.ft(INDEX_NAME).search(q, query_params={"vec": vec})

    out = []
    for doc in res.docs:
        # doc.score (혹은 doc.__dict__.get("score"))로 접근 가능
        out.append({"key": doc.id, "score": float(getattr(doc, "score", 0.0))})
    return out

