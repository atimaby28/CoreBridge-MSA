# AI Pipeline — CoreBridge

n8n + FastAPI + Ollama 기반 이력서 분석 & JD 매칭 시스템

## 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                      n8n Workflow                               │
│  Webhook → Parse → Analyze → Embed → Match → Score → Callback  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      FastAPI Service                            │
├─────────────────────────────────────────────────────────────────┤
│  POST /analyze/resume     # 이력서 분석 (요약 + 스킬 추출)       │
│  POST /embed              # 문장 임베딩 (벡터화)                │
│  POST /match/jd           # JD 매칭 (코사인 유사도)             │
│  POST /score              # 종합 스코어 산출                    │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
        ┌──────────┐   ┌──────────┐   ┌──────────┐
        │  Ollama  │   │  Redis   │   │Prometheus│
        │  llama3  │   │ Vector   │   │  메트릭  │
        └──────────┘   └──────────┘   └──────────┘
```

## 기술 스택

| 기술 | 용도 |
|------|------|
| **n8n** | 워크플로우 오케스트레이션 — 비동기 dispatch로 FastAPI 스레드 풀 고갈 방지 |
| **FastAPI** | AI 분석 REST API (:9001) |
| **Ollama (llama3)** | 로컬 LLM — 이력서 요약, 스킬 추출, 스코어링. `GEN_MODEL` 환경변수로 교체 가능 |
| **nomic-embed-text** | 문장 임베딩 모델 (768차원) |
| **Redis Stack** | Vector DB — 코사인 유사도 기반 JD 매칭 |
| **Prometheus** | 메트릭 수집 (ai_service_ollama_latency_ms 등) |

## 파이프라인 단계 (8단계)

| 단계 | 작업 | 평균 시간 |
|------|------|----------|
| 1 | Webhook 수신 | ~10ms |
| 2 | 이력서 텍스트 전처리 | ~100ms |
| 3 | LLM 요약 생성 | ~30,000ms |
| 4 | 문장 임베딩 (벡터화) | ~2,000ms |
| 5 | Redis Vector Search | ~30ms |
| 6 | LLM 스킬 추출 | ~25,000ms |
| 7 | LLM 스코어링 | ~25,000ms |
| 8 | 결과 콜백 | ~50ms |

**총 처리 시간**: ~80초 (LLM 작업이 95% 차지, CPU 추론 기준)

### Before vs After (n8n 비동기 전환)

| 엔드포인트 | Before (동기) | After (비동기 dispatch) | 개선율 |
|-----------|-------------|----------------------|--------|
| /summary | 25.1초 | 25ms | 99.9% |
| /skill_gap | 19.4초 | 38ms | 99.8% |
| /skills | 13.6초 | 36ms | 99.7% |
| /score | 10.4초 | 30ms | 99.7% |

> After = dispatch 응답 시간. LLM 처리는 n8n 워크플로우에서 백그라운드 실행.

## 실행

### 1. Ollama 설치 & 모델 다운로드

```bash
curl -fsSL https://ollama.com/install.sh | sh
ollama pull llama3
ollama pull nomic-embed-text
```

### 2. 환경 변수 설정

```bash
cd fastapi
cp .env.example .env
# 필요에 따라 GEN_MODEL, EMBEDDING_MODEL, REDIS_HOST 등 수정
```

### 3. Docker Compose 실행

```bash
cd fastapi
docker-compose up -d
# FastAPI(:9001) + n8n(:5678) 기동
```

## API 명세

### POST /analyze/resume

이력서 텍스트 분석 (요약 + 스킬 추출)

```json
// Request
{
  "resume_text": "이력서 전체 텍스트...",
  "job_posting_id": 123
}

// Response
{
  "summary": "5년차 백엔드 개발자, Java/Spring 전문...",
  "skills": ["Java", "Spring Boot", "Kubernetes", "Redis"],
  "experience_years": 5
}
```

### POST /match/jd

JD 매칭 점수 산출

```json
// Request
{
  "resume_embedding": [0.1, 0.2, ...],
  "job_posting_id": 123
}

// Response
{
  "similarity_score": 0.85,
  "matched_skills": ["Java", "Spring Boot"],
  "missing_skills": ["Kafka"]
}
```

## 모니터링

Grafana 대시보드에서 확인 가능한 메트릭:

- `ai_service_ollama_latency_ms`: Ollama 처리 지연
- `ai_requests_total`: 총 요청 수
- `ai_errors_total`: 에러 수

## 부하 테스트

k6 기반 Before/After 성능 비교 테스트:

```bash
# Before 테스트 (동기)
k6 run --env MODE=before deploy/load-test/k6-ai-pipeline.js

# After 테스트 (비동기)
k6 run --env MODE=after deploy/load-test/k6-ai-pipeline.js
```
