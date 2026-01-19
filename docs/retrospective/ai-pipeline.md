# AI 파이프라인 구축 회고

## 배경

채용 프로세스에서 **이력서 검토**는 가장 시간이 많이 걸리는 작업입니다.
- 기업 담당자가 수십~수백 개의 이력서를 일일이 검토
- 주관적 판단으로 일관성 부족
- 적합한 후보자를 놓칠 수 있음

**목표**: AI로 이력서를 자동 분석하고, JD와의 매칭 점수를 산출하여 채용 효율 향상

## 기술 선택

### 1. 왜 Ollama (로컬 LLM)인가?

| 대안 | 비용 | 프라이버시 | 속도 | 선택 |
|------|------|----------|------|------|
| OpenAI GPT-4 | 높음 | ❌ 외부 전송 | 빠름 | ❌ |
| Claude API | 높음 | ❌ 외부 전송 | 빠름 | ❌ |
| HuggingFace | 무료 | ✅ 로컬 | 복잡 | ❌ |
| **Ollama** | 무료 | ✅ 로컬 | 중간 | ✅ |

**선택 이유:**
- 이력서는 **개인정보**가 포함되어 외부 전송 부담
- 포트폴리오 프로젝트로 **비용 제약**
- Ollama는 설치/실행이 매우 간편

### 2. 왜 n8n인가?

| 대안 | 장점 | 단점 | 선택 |
|------|------|------|------|
| 직접 구현 | 자유도 높음 | 개발 시간 많음 | ❌ |
| Airflow | 강력함 | 과도한 복잡도 | ❌ |
| **n8n** | 시각적, 간편 | 커스텀 제한 | ✅ |

**선택 이유:**
- 8단계 파이프라인을 **시각적으로 관리**
- Webhook, HTTP 요청 등 **노코드로 연결**
- 에러 발생 시 **재시도 자동화**

### 3. 왜 Redis Vector Search인가?

| 대안 | 장점 | 단점 | 선택 |
|------|------|------|------|
| Pinecone | 관리형 | 비용, 외부 의존 | ❌ |
| Milvus | 고성능 | 운영 복잡 | ❌ |
| **Redis Stack** | 간편, 빠름 | 대규모 제한 | ✅ |

**선택 이유:**
- 이미 알림 시스템에서 **Redis 사용 중** (인프라 통합)
- 벡터 검색 응답 시간 **~30ms** (매우 빠름)
- 채용 공고 수가 수천 개 수준이면 충분

## 구현 과정

### 1단계: 프로토타입 (1주)

```python
# 가장 단순한 형태로 시작
def analyze_resume(text: str) -> dict:
    # Ollama 직접 호출
    response = ollama.generate(
        model="llama3.2",
        prompt=f"다음 이력서를 요약해줘: {text}"
    )
    return {"summary": response}
```

**문제점:**
- 처리 시간이 너무 오래 걸림 (~2분)
- 에러 발생 시 전체 실패
- 진행 상황 파악 불가

### 2단계: 파이프라인 분리 (1주)

```
Before: 한 번에 모든 작업 수행
After:  8단계로 분리, 각 단계 독립 실행
```

**개선점:**
- 단계별 **타임아웃 설정** 가능
- 실패 단계만 **재시도**
- Grafana에서 **병목 구간 시각화**

### 3단계: 모니터링 추가 (3일)

```python
# Prometheus Pushgateway로 메트릭 전송
from prometheus_client import push_to_gateway, Gauge

stage_duration = Gauge('ai_stage_duration_seconds', 'Stage duration', ['stage'])

def analyze_with_metrics(text: str):
    start = time.time()
    result = ollama.generate(...)
    stage_duration.labels(stage='summary').set(time.time() - start)
    push_to_gateway('pushgateway:9091', job='ai-pipeline', registry=registry)
```

**발견한 사실:**
- LLM 작업이 전체 시간의 **95% 차지**
- Redis 벡터 검색은 매우 빠름 (~30ms)
- 임베딩도 상대적으로 빠름 (~2초)

## 트러블슈팅

### 문제 1: LLM 응답 형식 불일치

**증상**: LLM이 JSON 대신 자연어로 응답
```
Expected: {"skills": ["Java", "Spring"]}
Actual:   "이 후보자의 스킬은 Java와 Spring입니다."
```

**해결**: 프롬프트에 **출력 형식 명시** + **few-shot 예시** 추가
```python
prompt = """
다음 이력서에서 기술 스택을 추출하세요.

출력 형식 (JSON만 출력):
{"skills": ["skill1", "skill2"]}

예시:
입력: "5년차 Java 개발자, Spring Boot와 Kubernetes 경험"
출력: {"skills": ["Java", "Spring Boot", "Kubernetes"]}

입력: {resume_text}
출력:
"""
```

### 문제 2: 긴 이력서 처리 실패

**증상**: 이력서가 길면 LLM 컨텍스트 초과
**해결**: 이력서를 **청크로 분할** 후 각각 요약, 최종 통합

### 문제 3: 메모리 부족

**증상**: Ollama가 GPU 메모리 부족으로 크래시
**해결**: 
- 모델을 `llama3.2:3b`로 경량화
- 동시 요청 수 제한 (세마포어)

## 성과

| 지표 | Before | After |
|------|--------|-------|
| 이력서 검토 시간 | 10분/건 | 2분/건 (자동) |
| 일관성 | 주관적 | 수치화된 점수 |
| 병목 파악 | 불가능 | Grafana로 실시간 |

## 배운 점

1. **LLM은 느리다** → 파이프라인 분리 + 비동기 처리 필수
2. **프롬프트 엔지니어링이 핵심** → 출력 형식 명시, few-shot 예시
3. **모니터링 없이 최적화 불가** → 메트릭 수집이 먼저
4. **로컬 LLM도 실용적** → 개인정보 처리, 비용 절감에 유리
