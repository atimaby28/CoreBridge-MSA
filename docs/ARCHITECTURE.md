# CoreBridge 아키텍처

## 전체 시스템 구성도

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Client Layer                                   │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                    Vue 3 + TypeScript + Tailwind                      │  │
│  │                    :5173 (dev) / Nginx :80 (prod)                    │  │
│  │                    ← SSE (EventSource :8010) 실시간 알림              │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    API Gateway (Spring Cloud Gateway)                       │
│                              :8000                                          │
│         JWT 중앙 검증 → X-User-Id / X-User-Email / X-User-Role 헤더 전달    │
│         WebFlux + Netty 기반 논블로킹 라우팅                                 │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     Backend Microservices (Spring Boot 3.4)                 │
├─────────────────────────────────────────────────────────────────────────────┤
│  :8001  │  :8002      │  :8003             │  :8004            │  :8005    │
│  user   │  jobposting │  jobposting-comment│  jobposting-view  │  job-like │
│         │  [Outbox]   │  [Outbox]          │  [Outbox]         │  [Outbox] │
├─────────┼─────────────┼────────────────────┼───────────────────┼───────────┤
│  :8006           │  :8007              │  :8008  │  :8009                   │
│  jobposting-hot  │  jobposting-read    │  resume │  apply                  │
│  [Kafka Consumer]│  [CB + CQRS Cache]  │         │  [State Machine+Outbox] │
├──────────────────┼─────────────────────┼─────────┼─────────────────────────┤
│  :8010              │  :8011    │  :8012                                    │
│  notification       │  schedule │  admin-audit                              │
│  [Kafka Consumer    │           │  [AuditFilter]                            │
│   + SSE + Pub/Sub]  │           │                                           │
└─────────────────────────────────────────────────────────────────────────────┘
         │                                    ▲
         │ Outbox → Kafka                     │ Kafka Consumer
         ▼                                    │
┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│    PostgreSQL    │    │   Redis Stack    │    │     Kafka        │
│   :5432          │    │ :6379            │    │   :9092          │
│  (서비스별 DB)    │    │ (Pub/Sub, Vector)│    │ (Outbox Pattern) │
└──────────────────┘    └──────────────────┘    └──────────────────┘
                                                          │
                          ┌───────────────────────────────┘
                          ▼
              ┌──────────────────┐
              │   AI Pipeline    │
              │  FastAPI :9001   │
              ├──────────────────┤
              │  Ollama :11434   │
              │  n8n    :5678    │
              │  Redis Vector DB │
              └──────────────────┘
```

---

## 서비스별 역할

### 핵심 비즈니스 서비스

| 포트 | 서비스 | 역할 | 적용 패턴 | 주요 API |
|------|--------|------|-----------|----------|
| 8000 | **gateway** | API Gateway, JWT 중앙 검증, 라우팅 | WebFlux + Netty | - |
| 8001 | **user** | 사용자 인증/권한 (USER/COMPANY/ADMIN) | JWT 발급 | POST /login, /signup |
| 8002 | **jobposting** | 채용공고 CRUD | Outbox | CRUD /api/v1/jobpostings |
| 8009 | **apply** | 지원서 관리 + 채용 프로세스 (State Machine) | Outbox | POST /apply, PATCH /transition |
| 8008 | **resume** | 이력서 관리 | - | CRUD /api/v1/resumes |
| 8011 | **schedule** | 면접 일정 관리 | - | CRUD /api/v1/schedules |
| 8010 | **notification** | 실시간 알림 (SSE + Kafka + Redis Pub/Sub) | Kafka Consumer | GET /sse, POST /notifications |

### 부가 서비스

| 포트 | 서비스 | 역할 | 적용 패턴 |
|------|--------|------|-----------|
| 8003 | **jobposting-comment** | 댓글/답글 | Outbox |
| 8004 | **jobposting-view** | 조회수 카운팅 | Outbox |
| 8005 | **jobposting-like** | 공고 좋아요/찜 | Outbox |
| 8006 | **jobposting-hot** | 인기 공고 집계 | Kafka Consumer + @Scheduled 배치 |
| 8007 | **jobposting-read** | 공고 상세 조회 (Aggregator) | Circuit Breaker + CQRS 캐시 |
| 8012 | **admin-audit** | 감사 로그 (모든 API 요청 자동 기록) | common AuditFilter |

---

## 공통 모듈 (common)

13개 서비스가 공유하는 핵심 공통 모듈:

```
common/
├── audit/                  # 감사 로그 AuditLoggingFilter
├── response/               # 표준 응답 포맷 (BaseResponse, BaseResponseStatus)
├── exception/              # 공통 예외 처리 (GlobalExceptionHandler)
├── security/               # GatewayAuthenticationFilter (X-User-Id/Email/Role → SecurityContext)
├── snowflake/              # 분산 ID 생성기 (Twitter Snowflake 알고리즘)
├── domain/                 # BaseTimeEntity
├── data-serializer/        # Jackson ObjectMapper 유틸 (Event JSON 직렬화)
├── event/                  # EventType, EventPayload 정의 (8종 이벤트)
└── outbox-message-relay/   # Outbox 엔티티 + Kafka 전송 + Redis Coordinator
```

---

## 이벤트 흐름 (Outbox → Kafka → Consumer)

```
┌──────────────────────────────────────────────────────────────────┐
│                      이벤트 생산자 (Outbox)                       │
│  jobposting, comment, view, like → 비즈니스 로직 + Outbox INSERT │
│  BEFORE_COMMIT: DB 저장 / AFTER_COMMIT: Kafka 비동기 발행        │
│  실패 시: 10초 Polling으로 미전송 재시도                           │
└──────────────────────────────┬───────────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────────┐
│                         Kafka (4 Topics)                         │
│  corebridge-jobposting │ corebridge-comment │ corebridge-like   │
│  corebridge-view       │ corebridge-notification                │
└──────────────────────────────┬───────────────────────────────────┘
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│ jobposting-read  │ │ jobposting-hot   │ │ notification     │
│ CQRS 읽기 모델    │ │ 인기 스코어 갱신   │ │ SSE Push 알림    │
│ 업데이트          │ │                  │ │                  │
└──────────────────┘ └──────────────────┘ └──────────────────┘
```

---

## AI 파이프라인

```
┌─────────────────────────────────────────────────────────────────┐
│                      n8n Workflow Engine                        │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐  │
│  │Webhook  │→│ Parse   │→│ Analyze │→│ Embed   │→│ Match   │  │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘  │
│       ↑                       │             │           │      │
│  Spring apply               ▼             ▼           ▼      │
│  (~25ms 즉시반환)      ┌──────────┐  ┌──────────┐ ┌──────────┐ │
│                        │  Ollama  │  │  nomic-  │ │  Redis   │ │
│                        │  llama3  │  │embed-text│ │  Vector  │ │
│                        └──────────┘  └──────────┘ └──────────┘ │
│                                                                 │
│  완료 시 Callback → Spring apply 서비스로 결과 전달              │
└─────────────────────────────────────────────────────────────────┘
```

### 파이프라인 단계

1. **Webhook**: Spring apply 서비스에서 이력서 제출 이벤트 수신 (~10ms)
2. **Parse**: 이력서 텍스트 추출/전처리 (~100ms)
3. **Analyze**: Ollama LLM으로 요약 생성 + 스킬 추출 (~30초)
4. **Embed**: nomic-embed-text로 문장 임베딩 벡터화 (~2초)
5. **Match**: Redis Vector Search로 JD 코사인 유사도 매칭 (~30ms)
6. **Score**: 종합 점수 산출 (~25초)
7. **Callback**: Spring apply 서비스로 결과 전달 (~50ms)

**총 처리 시간**: ~80초 (LLM 작업이 95% 차지)
**Spring 응답 시간**: ~25ms (Webhook POST 후 즉시 반환, 99.7% 개선)

---

## 실시간 알림 시스템

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   apply     │────▶│   Kafka     │────▶│ notification│
│ (상태변경)   │     │ (Outbox)    │     │  (Consumer) │
└─────────────┘     └─────────────┘     └─────────────┘
                                              │
                                    ┌─────────┴─────────┐
                                    ▼                   ▼
                              ┌──────────┐        ┌──────────┐
                              │ DB 저장   │        │Redis     │
                              │          │        │Pub/Sub   │
                              └──────────┘        └────┬─────┘
                                                       │
                                                       ▼ SSE
                                                 ┌──────────┐
                                                 │ Browser  │
                                                 │(EventSrc)│
                                                 └──────────┘
```

- **Outbox → Kafka**: 이벤트 발행 보장 (DB 트랜잭션 내 저장)
- **Redis Pub/Sub**: 다중 서버 환경에서 메시지 브로드캐스트
- **SSE**: 서버→클라이언트 단방향 실시간 전송 (15초 Heartbeat)

---

## 데이터 흐름

### 채용 지원 프로세스 (End-to-End)
```
1. User → apply 서비스: 지원 생성 (State: APPLIED)
2. apply → Outbox → Kafka(corebridge-notification) → notification: 지원 완료 알림
3. apply → n8n Webhook: AI 이력서 분석 요청 (비동기, ~25ms 즉시 반환)
4. n8n → FastAPI + Ollama + Redis Vector: 분석 실행 (~80초)
5. n8n → apply Callback: 분석 결과 + 매칭 점수 반환
6. Company → apply 서비스: 상태 전이 (APPLIED → DOCUMENT_REVIEW → ...)
7. apply → Outbox → Kafka → notification → SSE: 상태 변경 실시간 알림
```

### 공고 조회 (CQRS)
```
1. 쓰기: jobposting/comment/view/like → Outbox → Kafka → jobposting-read (캐시 업데이트)
2. 읽기: Frontend → Gateway → jobposting-read → ConcurrentHashMap 캐시 (2~5ms)
3. 캐시 미스: jobposting-read → CircuitBreaker → 원본 서비스 HTTP 호출 (Fallback 보호)
```

### 감사 로그
```
모든 API 요청 → common AuditLoggingFilter → admin-audit 서비스 → PostgreSQL
기록: 누가(X-User-Id), 언제(timestamp), 무엇을(Method + URI + Body), 결과(Status Code)
```
