# CoreBridge — MSA 기반 채용관리 플랫폼

> 6가지 핵심 아키텍처 패턴을 적용한 13개 마이크로서비스 채용 프로세스 관리 시스템

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-4FC08D.svg)](https://vuejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-blue.svg)](https://www.typescriptlang.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.110.0-009688.svg)](https://fastapi.tiangolo.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-336791.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

| 항목 | 수치 |
|------|------|
| Microservices | 13개 (Gateway 포함) |
| Architecture Patterns | 6개 (Outbox, CB, CQRS, AI Pipeline, Gateway, K8s CI/CD) |
| Java Files | 197 (source) + 29 (test) |
| AI Latency Reduction | 99.7% (동기→비동기 전환) |

---

## 🌐 Live Demo

**👉 [https://www.corebridge.cloud/home](https://www.corebridge.cloud/home)**

별도 설치 없이 위 링크에서 주요 기능을 바로 체험할 수 있습니다.
데모 버전은 MSA 13개 서비스를 단일 Spring Boot 앱으로 통합한 경량 버전입니다. → [CoreBridge-Demo 저장소](https://github.com/atimaby28/CoreBridge-Demo)

| 역할 | 이메일 | 비밀번호 |
|------|--------|----------|
| 관리자 | admin@demo.com | qwer1234 |
| 구직자 | user@demo.com | qwer1234 |
| 기업 | company@demo.com | qwer1234 |

---

## 🎯 프로젝트 소개

**CoreBridge**는 채용 프로세스를 **State Machine** 패턴으로 관리하고, **AI 기반 이력서 분석**을 통해 채용 효율을 높이는 MSA 기반 시스템입니다.

- 🏢 **기업**: 칸반보드로 지원자를 직관적으로 관리, AI 매칭 점수로 우선순위 파악
- 👤 **구직자**: 채용 진행 상태 확인, SSE 기반 실시간 Push 알림 수신
- 🤖 **AI**: 이력서 자동 분석, 스킬 추출, JD 매칭 점수 산출
- 🔒 **관리자**: 모든 활동에 대한 감사 로그 추적

---

## 🏗 6가지 핵심 아키텍처 패턴

각 패턴의 의사결정 과정, 시행착오, 회고는 [📄 포트폴리오 문서](docs/design/)와 설계 문서에서 상세히 다룹니다.

| # | 패턴 | 핵심 문제 | 해결 | 상세 |
|---|------|-----------|------|------|
| 01 | **Outbox Pattern** | DB 저장 성공 후 Kafka 발행 실패 → 데이터 불일치 | BEFORE_COMMIT에서 Outbox INSERT + AFTER_COMMIT 비동기 발행 + 10초 Polling 재시도. common 모듈에 공통화하여 4개 서비스 적용 | [설계문서](docs/design/CoreBridge-Outbox-설계문서.md) |
| 02 | **Circuit Breaker** | jobposting-read가 5개 서비스 동기 호출 → 1개 장애 시 연쇄 장애 | Resilience4j로 5개 서비스별 독립 CB 설정 + Fallback(0L) + CircuitBreakerEventConfig 상태 모니터링 | [설계문서](docs/design/CoreBridge-CircuitBreaker-설계문서.md) |
| 03 | **CQRS + Batch** | 목록 조회마다 4개 서비스 HTTP 호출 → 120~300ms 지연 | Kafka 이벤트 구독 → 로컬 ConcurrentHashMap 캐시. HTTP 호출 100% 제거, 응답 2~5ms | [설계문서](docs/design/CoreBridge-CQRS-Batch-설계문서.md) |
| 04 | **AI Pipeline** | Ollama LLM 호출 10~25초 → FastAPI 스레드 풀 고갈 | n8n Webhook 비동기 dispatch. 사용자 응답 25ms (99.7% 개선) | [설계문서](docs/design/CoreBridge-AI-Pipeline-설계문서.md) |
| 05 | **API Gateway** | 12개 서비스 각각 JWT 필터 중복 → Secret Key 분산 관리 | Gateway에서 JWT 중앙 검증 후 X-User-Id/Email/Role 헤더 전달. PUBLIC / Optional Auth / 필수 인증 3단계 분류 | [설계문서](docs/design/CoreBridge-API-Gateway-설계문서.md) |
| 06 | **K8s + CI/CD** | 13개 서비스 수동 배포 → 순서 실수, 롤백 불가 | K3s + Jenkins(Pod) + Kaniko 빌드 + Rolling Update (maxSurge=1, maxUnavailable=0) 무중단 배포 | [설계문서](docs/design/CoreBridge-K8s-CICD-설계문서.md) |

> **패턴 간 연계**: Outbox(01)가 이벤트 발행을 보장 → CQRS(03)가 안전하게 구독. Circuit Breaker(02)가 CQRS 캐시 미스 시 HTTP Fallback을 보호. apply 서비스의 상태 변경 → Outbox → Kafka → notification Consumer → SSE 실시간 알림까지 End-to-End 이벤트 파이프라인.

---

## 📐 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Frontend (Vue 3 + TypeScript)                        │
│                         :5173 / Nginx :80                              │
│                    ← SSE (EventSource :8010) 실시간 알림               │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    API Gateway (Spring Cloud Gateway)                   │
│                              :8000                                      │
│              JWT 중앙 검증 → X-User-Id/Email/Role 헤더 전달             │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     Backend Microservices (Spring Boot)                 │
├─────────────────────────────────────────────────────────────────────────┤
│  8001  │  8002  │  8003  │  8004  │  8005  │  8006  │  8007             │
│  user  │  job   │comment │  view  │  like  │  hot   │  read            │
│        │posting │        │        │        │        │ [CB+CQRS]        │
├────────┼────────┼────────┼────────┼────────┼────────┼──────────────────┤
│  8008  │  8009  │  8010  │  8011  │  8012  │                           │
│ resume │  apply │ notif  │schedule│ audit  │                           │
│        │[State] │        │        │        │                           │
└─────────────────────────────────────────────────────────────────────────┘
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

### 마이크로서비스 구성

| 포트 | 서비스 | 역할 | 적용 패턴 |
|------|--------|------|-----------|
| 8000 | **gateway** | API Gateway, JWT 중앙 검증, 라우팅 | Gateway, WebFlux+Netty |
| 8001 | **user** | 사용자 관리, 인증 (USER/COMPANY/ADMIN) | |
| 8002 | **jobposting** | 채용공고 CRUD | Outbox |
| 8003 | **jobposting-comment** | 댓글/답글 | Outbox |
| 8004 | **jobposting-view** | 조회수 카운팅 | Outbox |
| 8005 | **jobposting-like** | 공고 좋아요/찜 | Outbox |
| 8006 | **jobposting-hot** | 인기 공고 집계 (@Scheduled 배치) | Kafka Consumer |
| 8007 | **jobposting-read** | 공고 상세 조회 (Aggregator) | Circuit Breaker, CQRS 캐시 |
| 8008 | **resume** | 이력서 관리 | |
| 8009 | **apply** | ⭐ 지원서 관리 + 채용 프로세스 (State Machine) | Outbox |
| 8010 | **notification** | ⭐ 실시간 알림 (SSE + Kafka + Redis Pub/Sub) | Outbox Consumer → SSE Push |
| 8011 | **schedule** | 면접 일정 관리 | |
| 8012 | **admin-audit** | 감사 로그 (모든 API 요청 자동 기록) | common AuditFilter |

---

## 🛠 기술 스택

### Backend
| 기술 | 버전 | 설명 |
|------|------|------|
| Java | 21 | LTS |
| Spring Boot | 3.4.1 | 메인 프레임워크 |
| Spring Cloud Gateway | - | API Gateway (WebFlux + Netty) |
| Spring Data JPA | - | ORM |
| Resilience4j | - | Circuit Breaker (5개 서비스별 독립 설정) |
| PostgreSQL | 18+ | 메인 데이터베이스 (서비스별 DB 분리) |
| Redis Stack | 7+ | Pub/Sub, Vector DB |
| Kafka | 7.6 | 이벤트 스트리밍 (Outbox Pattern) |
| Gradle | 8.x | 멀티 모듈 빌드 (common + infra + 13 services) |

### AI/ML
| 기술 | 설명 |
|------|------|
| FastAPI | AI 분석 서비스 (:9001) |
| Ollama (llama3) | 로컬 LLM — 이력서 요약, 스킬 추출, 스코어링 |
| nomic-embed-text | 문장 임베딩 (768차원) |
| Redis Vector Search | 코사인 유사도 기반 JD 매칭 |
| n8n | 워크플로우 자동화 — 비동기 AI 파이프라인 오케스트레이션 (:5678) |

### Frontend
| 기술 | 버전 | 설명 |
|------|------|------|
| Vue | 3.x | Composition API |
| TypeScript | 5.x | 타입 안정성 |
| Pinia | 2.x | 상태 관리 |
| Tailwind CSS | 3.x | 스타일링 |
| Vite | 5.x | 빌드 도구 |

### Infra & DevOps
| 기술 | 설명 |
|------|------|
| Docker | 컨테이너화 (멀티 스테이지 빌드) |
| K3s | 경량 Kubernetes (WSL2 환경, 풀 K8s API 호환) |
| Jenkins | CI/CD — K3s Pod으로 배포, Kaniko 이미지 빌드 |
| Prometheus + Grafana | 실시간 메트릭 수집 + 모니터링 대시보드 |

> **배포 전략**: 현재 K3s 환경에서 **Rolling Update** (maxSurge=1, maxUnavailable=0) 적용. 이전 프로젝트에서 Blue-Green / Canary 배포 경험이 있으며, EKS 전환 시 Helm + ArgoCD(GitOps) 기반 Blue-Green 도입 계획.

### common 모듈 (`backend/common`)

13개 서비스가 공유하는 핵심 공통 모듈:

| 패키지 | 설명 |
|--------|------|
| `outboxmessagerelay` | **Outbox Pattern 공통 구현** — OutboxEventPublisher, MessageRelay(BEFORE_COMMIT 저장 + AFTER_COMMIT 발행 + @Scheduled Polling), Shard 기반 분산 처리. `@ConditionalOnProperty`로 서비스별 활성화 제어 |
| `event` | 이벤트 타입/페이로드 정의 — EventType, EventPayload, EventHandler 인터페이스. 7종 이벤트 (Created/Deleted/Updated/Viewed/Liked/Unliked/Comment/NotificationCreated) |
| `audit` | 감사 로그 공통 필터 — AuditLoggingFilter가 모든 API 요청을 자동 기록하여 admin-audit 서비스로 전송 |
| `security` | Gateway 인증 연동 — GatewayAuthenticationFilter가 X-User-Id/Email/Role 헤더를 SecurityContext로 변환 |
| `snowflake` | 분산 ID 생성기 — Twitter Snowflake 알고리즘 기반 고유 ID |
| `response` | 공통 응답 포맷 — BaseResponse, BaseResponseStatus |
| `exception` | 공통 예외 처리 — GlobalExceptionHandler |

---

## ⭐ 핵심 기능

### 1. State Machine 기반 채용 프로세스

```
APPLIED → DOCUMENT_REVIEW → DOCUMENT_PASS → INTERVIEW_1 → INTERVIEW_1_PASS
                │                    │              → INTERVIEW_2 → FINAL_REVIEW
                → DOCUMENT_FAIL      → CODING_TEST              → FINAL_PASS / FINAL_FAIL
```

Enum 기반 상태 전이 규칙으로 허용된 전이만 가능. 모든 상태 변경 이력 자동 추적.

### 2. AI 이력서 분석 & JD 매칭

n8n → FastAPI → Ollama(llama3) → Redis Vector 8단계 자동화 파이프라인.
이력서 요약, 스킬 추출, JD 코사인 유사도 매칭, 100점 만점 종합 스코어링.

### 3. 칸반보드 지원자 관리

드래그앤드롭으로 상태 변경, 체크박스 일괄 처리, AI 매칭 점수 기반 우선순위 정렬.

### 4. 실시간 알림 시스템 (SSE + Kafka)

```
apply(상태변경) → Outbox → Kafka(corebridge-notification)
                                    ↓
                          notification Consumer
                                    ↓
                    DB 저장 + Redis Pub/Sub → SseEmitter → 브라우저
```

채용 프로세스 상태 변경 시 구직자에게 **실시간 Push 알림** 전달.
Outbox Pattern으로 이벤트 발행을 보장하고, SseEmitter + 15초 Heartbeat로 안정적인 SSE 연결 유지.
프론트엔드에서 EventSource로 구독, 로그인/로그아웃 시 자동 연결/해제.

### 5. 감사 로그 (Audit Log)

common 모듈의 AuditLoggingFilter가 모든 API 요청을 자동 기록: 누가(X-User-Id), 언제(timestamp), 무엇을(Method + URI + Body), 결과(Status Code).

---

## 🚀 실행 방법

### 사전 요구사항

- Java 21+
- Node.js 20+
- PostgreSQL 18+
- Docker & Docker Compose

### 1. 인프라 실행 (Docker Compose)

```bash
git clone https://github.com/atimaby28/CoreBridge-MSA.git
cd CoreBridge-MSA

# PostgreSQL + Redis + Kafka 실행
docker-compose up -d
```

### 2. AI 파이프라인 실행

```bash
# Ollama 설치 & 모델 다운로드
curl -fsSL https://ollama.com/install.sh | sh
ollama pull llama3
ollama pull nomic-embed-text

# FastAPI + n8n 실행
cd ai/fastapi
docker-compose up -d
```

### 3. 백엔드 실행

```bash
# 데이터베이스 초기화
psql -U root -f scripts/init-db.sql

# 각 서비스 실행
cd backend
./gradlew :service:gateway:bootRun
./gradlew :service:user:bootRun
./gradlew :service:jobposting:bootRun
# ... (각 서비스별 실행)
```

### 4. 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
# → http://localhost:5173
```

### 5. 테스트 계정

| 역할 | 이메일 | 비밀번호 |
|------|--------|----------|
| 구직자 | user@test.com | qwer1234 |
| 기업 | company@test.com | qwer1234 |
| 관리자 | admin@test.com | qwer1234 |

---

## 📁 프로젝트 구조

```
CoreBridge-MSA/
├── README.md
├── LICENSE
├── docker-compose.yml              # 인프라 (PostgreSQL, Redis, Kafka)
│
├── docs/
│   ├── ARCHITECTURE.md              # 아키텍처 상세
│   ├── API.md                       # API 명세
│   ├── ERD.md                       # ERD 설명
│   ├── design/                      # 6가지 패턴 설계 문서
│   │   ├── CoreBridge-Outbox-설계문서.md
│   │   ├── CoreBridge-CircuitBreaker-설계문서.md
│   │   ├── CoreBridge-CQRS-Batch-설계문서.md
│   │   ├── CoreBridge-AI-Pipeline-설계문서.md
│   │   ├── CoreBridge-API-Gateway-설계문서.md
│   │   └── CoreBridge-K8s-CICD-설계문서.md
│   └── retrospective/              # 회고/트러블슈팅
│       ├── state-machine.md
│       ├── msa-architecture.md
│       ├── ai-pipeline.md
│       └── devops-cicd.md
│
├── backend/                         # Spring Boot MSA
│   ├── Dockerfile                   # 멀티 스테이지 빌드 (SERVICE_NAME ARG)
│   ├── Jenkinsfile                  # CI/CD 파이프라인 (Kaniko + Rolling Update)
│   ├── common/                      # 공통 모듈 (Outbox, Event, Audit, Security, Snowflake)
│   ├── infra/                       # 인프라 모듈 (Kafka, Redis)
│   └── service/
│       ├── gateway/                 # API Gateway (:8000) — WebFlux + Netty
│       ├── user/                    # 사용자 서비스 (:8001)
│       ├── jobposting/              # 채용공고 (:8002) — Outbox 적용
│       ├── jobposting-comment/      # 댓글 (:8003) — Outbox 적용
│       ├── jobposting-view/         # 조회수 (:8004) — Outbox 적용
│       ├── jobposting-like/         # 좋아요 (:8005) — Outbox 적용
│       ├── jobposting-hot/          # 인기 공고 (:8006) — @Scheduled 배치 집계
│       ├── jobposting-read/         # 조회 Aggregator (:8007) — CB + CQRS
│       ├── resume/                  # 이력서 (:8008)
│       ├── apply/                   # 지원 + State Machine (:8009) — Outbox 적용
│       ├── notification/            # 실시간 알림 (:8010) — Kafka Consumer + SSE + Redis Pub/Sub
│       ├── schedule/                # 일정 (:8011)
│       └── admin-audit/             # 감사 로그 (:8012)
│
├── frontend/                        # Vue 3 + TypeScript
│   ├── Dockerfile                   # Nginx 기반 프로덕션 빌드
│   ├── Jenkinsfile                  # 프론트엔드 CI/CD
│   └── nginx.conf                   # SPA 라우팅 + API 프록시
│
├── ai/                              # AI 파이프라인
│   └── fastapi/
│       ├── app.py                   # FastAPI 메인 앱 + Prometheus 메트릭
│       ├── llm.py                   # Ollama LLM 연동 (llama3)
│       ├── vector_store.py          # Redis Vector DB (nomic-embed-text)
│       ├── scoring.py               # 매칭 스코어링
│       ├── models.py                # Pydantic 모델
│       └── docker-compose.yml       # FastAPI + n8n + Ollama
│
├── deploy/                          # 배포 설정
│   ├── k3s/
│   │   ├── services/backend/        # 13개 서비스 K8s Deployment + Service YAML
│   │   ├── services/frontend/       # 프론트엔드 K8s 배포
│   │   ├── jenkins/                 # Jenkins K8s 배포 + RBAC
│   │   ├── monitoring/              # Prometheus + Grafana + 대시보드
│   │   └── dashboard/               # K8s Dashboard
│   └── load-test/                   # k6 부하 테스트 (AI Pipeline, CQRS + CB)
│
└── scripts/
    └── init-db.sql                  # PostgreSQL 스키마 초기화
```

---

## 🔗 관련 문서

- [아키텍처 상세](docs/ARCHITECTURE.md)
- [API 명세](docs/API.md)
- [ERD](docs/ERD.md)
- [AI 파이프라인](ai/README.md)
- **설계 문서 (6가지 패턴)**: [Outbox](docs/design/CoreBridge-Outbox-설계문서.md) · [Circuit Breaker](docs/design/CoreBridge-CircuitBreaker-설계문서.md) · [CQRS + Batch](docs/design/CoreBridge-CQRS-Batch-설계문서.md) · [AI Pipeline](docs/design/CoreBridge-AI-Pipeline-설계문서.md) · [API Gateway](docs/design/CoreBridge-API-Gateway-설계문서.md) · [K8s CI/CD](docs/design/CoreBridge-K8s-CICD-설계문서.md)
- **회고**: [State Machine](docs/retrospective/state-machine.md) · [MSA 아키텍처](docs/retrospective/msa-architecture.md) · [AI 파이프라인](docs/retrospective/ai-pipeline.md) · [CI/CD & K8s](docs/retrospective/devops-cicd.md)

---

## 👨‍💻 개발자

**양승우** · Backend Developer

- GitHub: [@atimaby28](https://github.com/atimaby28)

---

## 📄 라이선스

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
