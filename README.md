# CoreBridge - 실시간 채용관리 시스템

> Spring Boot MSA + Vue 3 + AI 기반 채용 프로세스 관리 플랫폼

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-4FC08D.svg)](https://vuejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-blue.svg)](https://www.typescriptlang.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.110.0-009688.svg)](https://fastapi.tiangolo.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-336791.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

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
- 👤 **구직자**: 실시간으로 채용 진행 상태 확인
- 🤖 **AI**: 이력서 자동 분석, 스킬 추출, JD 매칭 점수 산출
- 🔒 **관리자**: 모든 활동에 대한 감사 로그 추적

### 주요 특징

- **State Machine 패턴**: 복잡한 채용 단계를 안전하게 관리 (잘못된 상태 전이 원천 차단)
- **AI 파이프라인**: n8n + FastAPI + Ollama LLM 기반 8단계 자동화 파이프라인
- **MSA 아키텍처**: API Gateway + 13개 마이크로서비스로 구성된 확장 가능한 구조
- **실시간 알림**: 상태 변경 시 즉시 알림 (SSE + Redis Pub/Sub)
- **감사 로그**: 모든 API 호출 자동 기록 (누가, 언제, 무엇을)
- **모니터링**: Prometheus + Grafana 기반 실시간 성능 모니터링

---

## 🛠 기술 스택

### Backend
| 기술 | 버전 | 설명 |
|------|------|------|
| Java | 21 | LTS 버전 |
| Spring Boot | 3.4.1 | 메인 프레임워크 |
| Spring Cloud Gateway | - | API Gateway (JWT 인증 중앙화) |
| Spring Data JPA | - | ORM |
| Spring Batch | - | 배치 처리 (알림 재전송, 정리) |
| PostgreSQL | 18+ | 메인 데이터베이스 |
| Redis Stack | 7+ | Pub/Sub, 캐싱, Vector DB |
| Kafka | 7.6 | 이벤트 스트리밍 (Outbox Pattern) |
| Gradle | 8.x | 멀티 모듈 빌드 |

### AI/ML
| 기술 | 설명 |
|------|------|
| FastAPI | AI 분석 서비스 (:9001) |
| Ollama | 로컬 LLM (llama3.2) |
| nomic-embed-text | 문장 임베딩 |
| Redis Vector Search | 코사인 유사도 기반 JD 매칭 |
| n8n | 워크플로우 자동화 (:5678) |

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
| K3s | 경량 Kubernetes (WSL2 환경) |
| Jenkins | CI/CD (Kaniko 빌드 + Blue-Green 배포) |
| Prometheus | 메트릭 수집 |
| Grafana | 모니터링 대시보드 |

---

## 📐 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Frontend (Vue 3 + TypeScript)                        │
│                         :5173 / Nginx :80                              │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    API Gateway (Spring Cloud Gateway)                   │
│                              :8000                                      │
│                   JWT 인증 중앙화 + 라우팅                               │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     Backend Microservices (Spring Boot)                 │
├─────────────────────────────────────────────────────────────────────────┤
│  8001  │  8002  │  8003  │  8004  │  8005  │  8006  │  8007             │
│  user  │  job   │comment │  view  │  like  │  hot   │  read            │
├────────┼────────┼────────┼────────┼────────┼────────┼──────────────────┤
│  8008  │  8009  │  8011  │  8012  │  8013  │                           │
│ resume │  apply │ notif  │schedule│ audit  │                           │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
              ┌─────────────────────┼─────────────────────┐
              ▼                     ▼                     ▼
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

### AI 파이프라인 흐름 (8단계 자동화)

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│ Webhook │───▶│ FastAPI │───▶│ Ollama  │───▶│ Embed   │
│ (n8n)   │    │ 전처리  │    │ 요약    │    │ 벡터화  │
└─────────┘    └─────────┘    └─────────┘    └─────────┘
                                                  │
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌───▼─────┐
│ Spring  │◀───│ Scoring │◀───│ 스킬    │◀───│ Redis   │
│ Boot    │    │ LLM     │    │ 추출    │    │ Vector  │
└─────────┘    └─────────┘    └─────────┘    └─────────┘
```

1. **Webhook 수신**: n8n이 이력서 제출 이벤트 수신
2. **전처리**: FastAPI에서 이력서 텍스트 추출
3. **요약**: Ollama LLM으로 이력서 요약 생성
4. **벡터화**: nomic-embed-text로 문장 임베딩
5. **JD 매칭**: Redis Vector Search로 유사 공고 검색
6. **스킬 추출**: LLM으로 기술 스택 자동 추출
7. **스코어링**: AI가 종합 점수 산출
8. **결과 전송**: Spring Boot API로 결과 전달

### 마이크로서비스 구성

| 포트 | 서비스 | 설명 |
|------|--------|------|
| 8000 | **gateway** | API Gateway (JWT 인증 중앙화, 라우팅) |
| 8001 | **user** | 사용자 관리, 인증 (USER/COMPANY/ADMIN) |
| 8002 | **jobposting** | 채용공고 CRUD |
| 8003 | **jobposting-comment** | 댓글/답글 |
| 8004 | **jobposting-view** | 조회수 카운팅 |
| 8005 | **jobposting-like** | 공고 좋아요/찜 |
| 8006 | **jobposting-hot** | 인기 공고 집계 |
| 8007 | **jobposting-read** | 공고 상세 조회 (Aggregator) |
| 8008 | **resume** | 이력서 관리 |
| 8009 | **apply** | ⭐ 지원서 관리 + 채용 프로세스 (State Machine) |
| 8011 | **notification** | 실시간 알림 (SSE + Redis Pub/Sub) |
| 8012 | **schedule** | 면접 일정 관리 |
| 8013 | **admin-audit** | 감사 로그 |

### 인프라 서비스

| 포트 | 서비스 | 설명 |
|------|--------|------|
| 5432 | **PostgreSQL** | 메인 데이터베이스 (서비스별 DB 분리) |
| 6379 | **Redis Stack** | Pub/Sub, 캐싱, Vector DB |
| 9092 | **Kafka** | 이벤트 스트리밍 (Outbox Pattern) |
| 9001 | **FastAPI** | AI 분석 서비스 |
| 11434 | **Ollama** | 로컬 LLM |
| 5678 | **n8n** | 워크플로우 자동화 |
| 9090 | **Prometheus** | 메트릭 수집 |
| 3000 | **Grafana** | 모니터링 대시보드 |

---

## ⭐ 핵심 기능

### 1. Process 기반 채용 상태관리 (State Machine)

```
APPLIED (지원완료)
    ↓
DOCUMENT_REVIEW (서류검토중)
    ↓
┌───────────────┬───────────────┐
↓               ↓               
DOCUMENT_PASS   DOCUMENT_FAIL (종료)
    ↓               
    ├─── CODING_TEST ─── CODING_PASS/FAIL
    │
    └─── INTERVIEW_1 ─── INTERVIEW_1_PASS/FAIL
                              ↓
                         INTERVIEW_2 ─── INTERVIEW_2_PASS/FAIL
                              ↓
                         FINAL_REVIEW
                              ↓
                    FINAL_PASS / FINAL_FAIL
```

**특징:**
- 허용된 상태 전이만 가능 (Enum 기반 규칙 정의)
- 모든 상태 변경 이력 자동 추적
- 잘못된 전이 시 예외 발생

### 2. AI 이력서 분석 & JD 매칭

| 기능 | 기술 | 설명 |
|------|------|------|
| 이력서 요약 | Ollama LLM | 긴 이력서를 핵심 내용으로 요약 |
| 스킬 추출 | Ollama LLM | 기술 스택 자동 추출 (Java, Spring 등) |
| JD 매칭 | Redis Vector | 코사인 유사도 기반 적합 공고 추천 |
| 종합 스코어 | AI Scoring | 100점 만점 매칭 점수 산출 |

**성능 메트릭 (Grafana 모니터링):**
- Redis 벡터 검색: ~30ms (매우 빠름)
- 임베딩: ~2,000ms
- LLM 작업: ~80,000ms (병목 구간 → 비동기 처리로 개선)

### 3. 칸반보드 지원자 관리

| 기능 | 설명 |
|------|------|
| 드래그앤드롭 | 지원자 카드를 끌어서 상태 변경 |
| 일괄 처리 | 체크박스로 여러 지원자 동시 합격/불합격 |
| AI 점수 표시 | 매칭 점수 기반 우선순위 파악 |
| 필터링 | 공고별, 상태별, 점수별 필터 |

### 4. 실시간 알림 시스템

```
[상태 변경] → [Redis Pub/Sub] → [SSE] → [브라우저 알림]
```

- 상태 변경 시 즉시 알림 발송
- 면접 일정 리마인더
- Spring Batch로 실패 알림 재전송 (5분마다)

### 5. 감사 로그 (Audit Log)

모든 API 요청 자동 기록:
- **누가**: X-User-Id, X-User-Email
- **언제**: timestamp
- **무엇을**: HTTP Method, URI, Request/Response Body
- **결과**: HTTP Status Code

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
ollama pull llama3.2
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
│   ├── design/                      # 설계 문서
│   │   ├── CoreBridge-AI-Pipeline-설계문서.md
│   │   ├── CoreBridge-API-Gateway-설계문서.md
│   │   ├── CoreBridge-CQRS-Batch-설계문서.md
│   │   ├── CoreBridge-CircuitBreaker-설계문서.md
│   │   ├── CoreBridge-K8s-CICD-설계문서.md
│   │   └── CoreBridge-Outbox-설계문서.md
│   └── retrospective/              # 회고/트러블슈팅
│       ├── state-machine.md
│       ├── msa-architecture.md
│       ├── ai-pipeline.md
│       └── devops-cicd.md
│
├── backend/                         # Spring Boot MSA
│   ├── Dockerfile                   # 멀티 스테이지 빌드 (서비스명 ARG)
│   ├── Jenkinsfile                  # CI/CD 파이프라인
│   ├── common/                      # 공통 모듈 (JWT, Audit, Exception)
│   ├── infra/                       # 인프라 모듈 (Kafka, Redis)
│   └── service/
│       ├── gateway/                 # API Gateway (:8000)
│       ├── user/                    # 사용자 서비스 (:8001)
│       ├── jobposting/              # 채용공고 (:8002)
│       ├── jobposting-comment/      # 댓글 (:8003)
│       ├── jobposting-view/         # 조회수 (:8004)
│       ├── jobposting-like/         # 좋아요 (:8005)
│       ├── jobposting-hot/          # 인기 공고 (:8006)
│       ├── jobposting-read/         # 조회 Aggregator (:8007)
│       ├── resume/                  # 이력서 (:8008)
│       ├── apply/                   # 지원 + State Machine (:8009)
│       ├── notification/            # 알림 (:8011)
│       ├── schedule/                # 일정 (:8012)
│       └── admin-audit/             # 감사 로그 (:8013)
│
├── frontend/                        # Vue 3 + TypeScript
│   ├── Dockerfile                   # Nginx 기반 프로덕션 빌드
│   ├── Jenkinsfile                  # 프론트엔드 CI/CD
│   └── nginx.conf                   # SPA 라우팅 + API 프록시
│
├── ai/                              # AI 파이프라인
│   └── fastapi/
│       ├── app.py                   # FastAPI 메인 앱
│       ├── llm.py                   # Ollama LLM 연동
│       ├── vector_store.py          # Redis Vector DB
│       ├── scoring.py               # 매칭 스코어링
│       ├── models.py                # Pydantic 모델
│       ├── Dockerfile               # AI 서비스 컨테이너
│       └── docker-compose.yml       # FastAPI + n8n
│
├── deploy/                          # 배포 설정
│   ├── k3s/
│   │   ├── services/                # K8s Deployment + Service YAML
│   │   ├── jenkins/                 # Jenkins K8s 배포
│   │   ├── monitoring/              # Prometheus + Grafana
│   │   └── dashboard/               # K8s Dashboard
│   └── load-test/                   # 부하 테스트 스크립트
│
├── scripts/
│   └── init-db.sql                  # PostgreSQL Enum & 스키마 초기화
│
└── .github/
    └── pull_request_template.md
```

---

## 📝 기술적 의사결정

### 1. State Machine 패턴 선택

| 대안 | 장점 | 단점 | 선택 |
|------|------|------|------|
| if-else | 단순 | 상태 늘어나면 복잡 | ❌ |
| State 패턴 (GoF) | 확장성 | 클래스 폭발 | ❌ |
| **Enum 기반 State Machine** | 간결 + 타입 안전 | - | ✅ |

→ [상세 회고](docs/retrospective/state-machine.md)

### 2. Ollama 로컬 LLM 선택

| 대안 | 장점 | 단점 | 선택 |
|------|------|------|------|
| OpenAI API | 성능 우수 | 비용, 데이터 외부 전송 | ❌ |
| HuggingFace | 무료 | 직접 호스팅 필요, 복잡 | ❌ |
| **Ollama** | 로컬, 무료, 간편 | 성능 제한 | ✅ |

→ 로컬 LLM으로 **비용 절감**, **데이터 프라이버시 보장**, **빠른 반복 개발**

### 3. MSA vs Monolith

| 항목 | MSA | Monolith |
|------|-----|----------|
| 확장성 | ✅ 서비스별 독립 스케일링 | ❌ 전체 스케일링 |
| 복잡도 | ❌ 서비스간 통신 | ✅ 단순 |
| 배포 | ✅ 독립 배포 | ❌ 전체 배포 |

→ 채용 시스템 특성상 기능별 트래픽 차이가 커서 MSA 선택

### 4. 실시간 알림: SSE + Redis Pub/Sub

| 대안 | 장점 | 단점 | 선택 |
|------|------|------|------|
| WebSocket | 양방향 | 연결 관리 복잡 | ❌ |
| Long Polling | 단순 | 리소스 낭비 | ❌ |
| **SSE + Redis** | 단방향 충분, 확장성 | - | ✅ |

→ 알림은 서버→클라이언트 단방향이므로 SSE가 적합, Redis Pub/Sub로 다중 서버 지원

---

## 🔗 관련 문서

- [아키텍처 상세](docs/ARCHITECTURE.md)
- [API 명세](docs/API.md)
- [ERD](docs/ERD.md)
- [AI 파이프라인](ai/README.md)
- **설계 문서**
  - [AI Pipeline 설계](docs/design/CoreBridge-AI-Pipeline-설계문서.md)
  - [API Gateway 설계](docs/design/CoreBridge-API-Gateway-설계문서.md)
  - [CQRS + Batch 설계](docs/design/CoreBridge-CQRS-Batch-설계문서.md)
  - [Circuit Breaker 설계](docs/design/CoreBridge-CircuitBreaker-설계문서.md)
  - [K8s CI/CD 설계](docs/design/CoreBridge-K8s-CICD-설계문서.md)
  - [Outbox Pattern 설계](docs/design/CoreBridge-Outbox-설계문서.md)
- **회고/트러블슈팅**
  - [State Machine 패턴 도입기](docs/retrospective/state-machine.md)
  - [MSA 아키텍처 설계](docs/retrospective/msa-architecture.md)
  - [AI 파이프라인 구축](docs/retrospective/ai-pipeline.md)
  - [CI/CD & Kubernetes](docs/retrospective/devops-cicd.md)

---

## 🛤 로드맵

- [x] MVP - 기본 채용 프로세스 (State Machine)
- [x] 칸반보드 UI (드래그앤드롭)
- [x] 실시간 알림 (SSE + Redis Pub/Sub)
- [x] AI 이력서 분석 (Ollama LLM)
- [x] JD 매칭 (Redis Vector Search)
- [x] 감사 로그
- [x] API Gateway (JWT 인증 중앙화)
- [x] Kubernetes + Jenkins CI/CD
- [x] Prometheus + Grafana 모니터링
- [x] Outbox Pattern (Kafka 기반 이벤트 발행)
- [ ] Circuit Breaker (Resilience4j)
- [ ] CQRS + Batch (읽기 모델 분리)

---

## 👨‍💻 개발자

**양승우**

- GitHub: [@atimaby28](https://github.com/atimaby28)

---

## 📄 라이선스

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
