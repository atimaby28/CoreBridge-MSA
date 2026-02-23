# CoreBridge AI Pipeline 설계 문서

## 1. 개요

### 목적
이력서-채용공고 양방향 매칭을 위한 AI 분석 파이프라인.
FastAPI + Ollama(LLM) + Redis Vector Search 기반으로 구현.
n8n 워크플로우로 비동기 오케스트레이션하여 MSA 스레드 풀 고갈을 방지.

### 구현 상태: ✅ 완료

---

## 1-1. 왜 n8n 비동기 오케스트레이션인가?

LLM 추론(~80초)이 Spring 스레드를 블로킹하는 문제를 해결하기 위해 3가지 대안을 비교했다.

| 대안 | 동작 방식 | 문제점 | 판정 |
|------|----------|--------|------|
| **Spring @Async** | 별도 스레드 풀에서 LLM 호출 | 스레드가 다른 풀로 이동할 뿐, JVM 내부에서 80초 블로킹은 동일. 스레드 풀 고갈 위험 잔존 | ❌ |
| **Kafka Worker** | Kafka Consumer가 별도 프로세스로 LLM 처리 | 8개 AI 토픽 필요, Consumer 관리 복잡도 증가. 워크플로우 단계별 오케스트레이션 어려움 | ❌ |
| **n8n Webhook** | HTTP Webhook으로 즉시 응답(~100ms) 후 n8n이 비동기로 8단계 처리 | JVM과 완전 분리. LLM 80초 블로킹이 Spring에 영향 없음. 워크플로우 시각적 관리 가능 | ✅ |

### 왜 Ollama (로컬 LLM)인가?

| 대안 | 비용 | 프라이버시 | 속도 | 판정 |
|------|------|----------|------|------|
| OpenAI GPT-4 | 높음 (토큰당 과금) | ❌ 이력서 외부 전송 | 빠름 | ❌ |
| Claude API | 높음 | ❌ 이력서 외부 전송 | 빠름 | ❌ |
| HuggingFace Transformers | 무료 | ✅ 로컬 | 모델별 상이, 설정 복잡 | ❌ |
| **Ollama (llama3)** | 무료 | ✅ 로컬 | M1 칩 GPU 가속 | ✅ |

**선택 근거**: 채용 플랫폼 특성상 이력서에 개인정보가 포함되므로 외부 API 전송은 부적절. Ollama는 로컬 실행으로 프라이버시 보장 + 무료.

### 하드웨어 제약과 대응
- **환경**: iMac M1 (2021) / 16GB RAM / macOS
- **제약**: GPU 메모리 부족 시 Ollama 크래시 발생
- **대응**: AI 컴포넌트를 K3s 외부(Docker Compose)로 분리하여 리소스 경합 방지

---

## 2. 아키텍처

### Before (동기 처리)
```
[ApplyService] → HTTP 동기 호출 → [FastAPI] → Ollama(~80초)
                  ↑ 스레드 블로킹 (80초간 점유)
                  → 동시 지원 10건 → 스레드 풀 고갈 → 전체 서비스 장애
```
**문제점**: LLM 처리(~80초)가 Spring 스레드를 블로킹 → 동시 처리 불가

### After (n8n 비동기 처리)
```
[ApplyService] → Webhook POST → [n8n] (즉시 응답, ~100ms)
                                  ↓ 비동기
                          [n8n Workflow 8단계]
                              ↓
                          [FastAPI + Ollama + Redis]
                              ↓ 완료 시
                          [Callback → ApplyService]
```
**개선**: 99.7% 응답시간 단축 (80초 → 100ms), 스레드 풀 고갈 방지

---

## 3. 기술 스택

| 기술 | 역할 | 비고 |
|------|------|------|
| **FastAPI** | AI 분석 REST API 서버 | Python, 포트 9001 |
| **Ollama** | 로컬 LLM 추론 | llama3 (생성), nomic-embed-text (임베딩) |
| **Redis Stack** | Vector DB | 코사인 유사도 검색, FT.SEARCH |
| **n8n** | 워크플로우 오케스트레이션 | Webhook → 8단계 비동기 처리 |
| **Prometheus** | 메트릭 수집 | FastAPI에서 직접 노출 (/metrics) |

---

## 4. FastAPI 엔드포인트

| 엔드포인트 | 태그 | 용도 | Ollama 사용 |
|-----------|------|------|------------|
| `POST /save_resume` | Resume | 이력서 임베딩 → Redis 저장 | 임베딩만 |
| `POST /save_jobposting` | Jobposting | 채용공고 임베딩 → Redis 저장 | 임베딩만 |
| `POST /match_candidates` | Matching | JD 기반 후보자 매칭 (회사용) | 임베딩만 |
| `POST /match_jobpostings` | Matching | 이력서 기반 공고 추천 (구직자용) | 임베딩만 |
| `POST /score` | Scoring | 후보자 상세 점수 계산 | 임베딩+스킬추출 |
| `POST /skill_gap` | Analysis | 스킬 갭 분석 (구직자용) | 스킬추출 |
| `POST /skills` | Skills | 텍스트에서 기술 스택 추출 | LLM 생성 |
| `POST /summary` | Summary | 텍스트 요약 (한국어) | LLM 생성 |
| `GET /metrics` | - | Prometheus 메트릭 노출 | - |

---

## 5. 핵심 모듈

### 5-1. 임베딩 (app.py → embed())
```python
def embed(text: str):
    if EMBEDDING_BACKEND == "ollama":
        resp = ollama.embeddings(model="nomic-embed-text", prompt=text)
        return resp["embedding"]
    else:
        # OpenAI fallback
        resp = openai_client.embeddings.create(model=EMBEDDING_MODEL, input=text)
        return resp.data[0].embedding
```

### 5-2. LLM 추론 (llm.py)
- `summarize()`: 한국어 5줄 요약 (프롬프트 엔지니어링)
- `extract_skills()`: JSON 배열로 기술 스택 추출 + regex fallback

### 5-3. 벡터 검색 (vector_store.py)
- Redis `FT.CREATE` 인덱스 (HNSW 알고리즘)
- `save_resume()` / `save_jobposting()`: 벡터 + 메타데이터 저장
- `search_similar_resumes()` / `search_similar_jobpostings()`: KNN 검색

### 5-4. 스코어링 (scoring.py)
```python
total_score = skill_score(40점) + similarity_score(40점) + bonus(20점)
# 등급: A(85+), B(70+), C(55+), D(40+), F(<40)
```

---

## 6. Prometheus 메트릭

| 메트릭 | 타입 | 설명 |
|--------|------|------|
| `ai_service_requests_total` | Counter | 엔드포인트별 총 요청 수 |
| `ai_service_request_latency_seconds` | Histogram | 엔드포인트별 응답시간 |
| `ai_service_ollama_latency_ms` | Gauge | Ollama LLM 처리 시간 |
| `ai_service_embedding_latency_ms` | Gauge | 임베딩 생성 시간 |
| `ai_service_match_latency_ms` | Gauge | 매칭 처리 시간 |
| `ai_service_score_latency_ms` | Gauge | 스코어링 처리 시간 |
| `ai_service_redis_latency_ms` | Gauge | Redis 통신 시간 |
| `ai_service_errors_total` | Counter | 에러 수 |

---

## 7. n8n 워크플로우 (8단계)

| 단계 | 작업 | 평균 시간 | 호출 대상 |
|------|------|----------|----------|
| 1 | Webhook 수신 | ~10ms | - |
| 2 | 이력서 텍스트 전처리 | ~100ms | - |
| 3 | LLM 요약 생성 | ~30,000ms | FastAPI /summary |
| 4 | 문장 임베딩 (벡터화) | ~2,000ms | FastAPI /save_resume |
| 5 | Redis Vector Search | ~30ms | FastAPI /match_candidates |
| 6 | LLM 스킬 추출 | ~25,000ms | FastAPI /skills |
| 7 | LLM 스코어링 | ~25,000ms | FastAPI /score |
| 8 | 결과 콜백 | ~50ms | Spring ApplyService |

**총 처리 시간**: ~80초 (LLM 작업이 95% 차지)
**Spring 응답시간**: ~100ms (Webhook POST 후 즉시 반환)

---

## 8. 인프라 구성

### Docker Compose (K3s 외부 분리)
```yaml
services:
  redis:
    image: redis/redis-stack-server
    ports: ["6379:6379"]
  fastapi:
    build: ./ai/fastapi
    ports: ["9001:9001"]
    environment:
      - OLLAMA_HOST=http://host.docker.internal:11434
      - REDIS_HOST=redis
```

**분리 이유**: Ollama가 CPU 집약적이라 K3s 클러스터와 리소스 경합 방지
(로컬PC: iMac M1 (2021) / 16GB RAM)

---

## 9. 포트폴리오 캡처 항목

| 항목 | Before | After |
|------|--------|-------|
| 응답시간 | ~80초 (동기) | ~100ms (비동기 Webhook) |
| 스레드 풀 | 10건 동시 처리 시 고갈 | 무제한 (비동기 큐) |
| Grafana 패널 | 없음 | 6개 메트릭 패널 |

---

## 10. 파일 구조

```
ai/
├── README.md
└── fastapi/
    ├── Dockerfile
    ├── docker-compose.yml
    ├── requirements.txt
    ├── .env.example
    ├── app.py           # FastAPI 메인 (엔드포인트 + Prometheus)
    ├── models.py         # Pydantic 요청/응답 모델
    ├── llm.py            # Ollama LLM 호출 (요약, 스킬추출)
    ├── scoring.py        # 규칙 기반 스코어링
    └── vector_store.py   # Redis Vector 저장/검색
```
