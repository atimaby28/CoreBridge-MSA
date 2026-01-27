import os
import re
import json
import ollama

GEN_MODEL = os.getenv("GEN_MODEL", "tinyllama")  # override by env in app.py

def summarize(text: str) -> str:
    prompt = f"""
다음 내용을 5줄 bullet point로 한국어 요약해줘.
불필요한 내용 제외하고 핵심 정보만 포함:

{text}
"""
    resp = ollama.generate(model=GEN_MODEL, prompt=prompt)
    return resp.get("response", "").strip()

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
    prompt = f"""
아래 문서에서 기술 스택(언어/프레임워크/DB/DevOps 도구)만 JSON 배열로 출력.
예: ["Java","Spring","AWS"]
추측 금지, 문서에 실제 등장한 기술만:

{text}
"""
    try:
        resp = ollama.generate(model=GEN_MODEL, prompt=prompt).get("response","")
        arr = re.search(r"\[.*\]", resp, re.DOTALL)
        if arr:
            parsed = json.loads(arr.group(0))
            return [str(x).strip() for x in parsed if isinstance(x,(str,))]
    except Exception:
        pass

    hits = set(m.group(0) for m in SKILL_REGEX.finditer(text or ""))
    normalized = set()
    for h in hits:
        for base in CANONICAL_SKILLS:
            if h.lower() == base.lower():
                normalized.add(base); break
    return sorted(normalized)
