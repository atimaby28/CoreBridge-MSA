import os
import re
import json
import httpx
import ollama

GEN_MODEL = os.getenv("GEN_MODEL", "llama3")  # override by env in app.py

# Ollama 클라이언트에 타임아웃 설정 (60초)
_ollama_client = ollama.Client(
    host=os.getenv("OLLAMA_HOST", "http://host.docker.internal:11434"),
    timeout=httpx.Timeout(60.0, connect=5.0),
)

def summarize(text: str) -> str:
    prompt = f"""You are a Korean-language assistant. You MUST respond ONLY in Korean (한국어).
Do NOT use English at all. Summarize the following content as 5 bullet points in Korean.
Include only key information, exclude unnecessary details.

[Content to summarize]
{text}

[Instructions]
- Output MUST be in Korean only (한국어로만 작성)
- Use bullet points (•)
- Maximum 5 points
- Be concise and specific
"""
    try:
        resp = _ollama_client.generate(model=GEN_MODEL, prompt=prompt)
        return resp.get("response", "").strip()
    except Exception as e:
        print(f"[LLM] summarize timeout/error: {e}")
        return ""

CANONICAL_SKILLS = [
    "Java","Spring","Spring Boot","JPA","Hibernate","MySQL","MariaDB","PostgreSQL",
    "Redis","Kafka","RabbitMQ","Elasticsearch","Docker","Kubernetes","AWS","GCP","Azure",
    "Jenkins","GitHub Actions","React","Vue","TypeScript","Python","FastAPI","Django","자바","스프링","스프링 부트","자바 퍼시스턴스 API","하이버네이트","마이SQL","마리아DB","포스트그레SQL","레디스","카프카","래빗MQ","엘라스틱서치","도커","쿠버네티스","아마존 웹 서비스","구글 클라우드 플랫폼","마이크로소프트 애저","젠킨스","깃허브 액션즈","리액트","뷰","타입스크립트","파이썬","패스트API","장고"
]

SKILL_REGEX = re.compile(
    r"|".join([re.escape(s) for s in sorted(CANONICAL_SKILLS, key=len, reverse=True)]),
    re.IGNORECASE
)

def extract_skills(text: str) -> list[str]:
    prompt = f"""Extract ONLY technology stack names (programming languages, frameworks, databases, DevOps tools) from the document below.
Output as a JSON array of strings. Only include technologies that actually appear in the text. Do NOT guess.

Example output: ["Java","Spring Boot","AWS","Docker"]

Document:
{text}

JSON array:"""
    try:
        resp = _ollama_client.generate(model=GEN_MODEL, prompt=prompt).get("response","")
        arr = re.search(r"\[.*\]", resp, re.DOTALL)
        if arr:
            parsed = json.loads(arr.group(0))
            return [str(x).strip() for x in parsed if isinstance(x,(str,))]
    except Exception as e:
        print(f"[LLM] extract_skills timeout/error, using regex fallback: {e}")

    # Regex fallback — LLM 실패 시에도 스킬 추출 가능
    hits = set(m.group(0) for m in SKILL_REGEX.finditer(text or ""))
    normalized = set()
    for h in hits:
        for base in CANONICAL_SKILLS:
            if h.lower() == base.lower():
                normalized.add(base); break
    return sorted(normalized)
