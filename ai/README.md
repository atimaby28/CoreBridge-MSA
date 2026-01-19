# AI Pipeline - CoreBridge

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
        │   LLM    │   │ Vector   │   │Pushgateway│
        └──────────┘   └──────────┘   └──────────┘
```

## 기술 스택

| 기술 | 용도 |
|------|------|
| **n8n** | 워크플로우 오케스트레이션 |
| **FastAPI** | AI 분석 REST API |
| **Ollama** | 로컬 LLM (llama3.2) |
| **nomic-embed-text** | 문장 임베딩 모델 |
| **Redis Stack** | Vector DB (코사인 유사도 검색) |
| **Prometheus** | 메트릭 수집 |

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

**총 처리 시간**: ~80초 (LLM 작업이 95% 차지)

## 실행

### 1. Ollama 설치 & 모델 다운로드

```bash
# Ollama 설치
curl -fsSL https://ollama.com/install.sh | sh

# 모델 다운로드
ollama pull llama3.2
ollama pull nomic-embed-text
```

### 2. FastAPI 서비스 실행

```bash
cd fastapi
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000
```

### 3. n8n 워크플로우 임포트

```bash
# n8n 실행
docker run -d -p 5678:5678 n8nio/n8n

# workflows/ 폴더의 JSON 파일 임포트
```

## API 명세

### POST /analyze/resume

이력서 텍스트 분석 (요약 + 스킬 추출)

**Request:**
```json
{
  "resume_text": "이력서 전체 텍스트...",
  "job_posting_id": 123
}
```

**Response:**
```json
{
  "summary": "5년차 백엔드 개발자, Java/Spring 전문...",
  "skills": ["Java", "Spring Boot", "Kubernetes", "Redis"],
  "experience_years": 5
}
```

### POST /match/jd

JD 매칭 점수 산출

**Request:**
```json
{
  "resume_embedding": [0.1, 0.2, ...],
  "job_posting_id": 123
}
```

**Response:**
```json
{
  "similarity_score": 0.85,
  "matched_skills": ["Java", "Spring Boot"],
  "missing_skills": ["Kafka"]
}
```

## 모니터링

Grafana 대시보드에서 확인 가능한 메트릭:

- `ai_pipeline_duration_seconds`: 전체 파이프라인 처리 시간
- `ai_stage_duration_seconds{stage="summary"}`: 단계별 처리 시간
- `ai_requests_total`: 총 요청 수
- `ai_errors_total`: 에러 수

## 성능 최적화 방안

| 병목 | 현재 | 개선 방안 |
|------|------|----------|
| LLM 요약 | ~30초 | 모델 경량화, GPU 활용 |
| LLM 스킬추출 | ~25초 | 프롬프트 최적화, 캐싱 |
| LLM 스코어링 | ~25초 | 배치 처리, 비동기화 |
