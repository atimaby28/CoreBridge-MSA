# Backend — CoreBridge MSA

Spring Boot 3.4.1 기반 13개 마이크로서비스 아키텍처

## 프로젝트 구조

```
backend/
├── settings.gradle          # 멀티모듈 설정 (common + infra + 13 services)
├── build.gradle             # 루트 빌드 설정
├── Dockerfile               # 멀티 스테이지 빌드 (SERVICE_NAME ARG)
├── Jenkinsfile              # CI/CD (Kaniko 빌드 + K3s Rolling Update)
│
├── common/                  # 공통 모듈 (13개 서비스 공유)
│   ├── outboxmessagerelay/ # Outbox Pattern — BEFORE_COMMIT 저장 + AFTER_COMMIT 발행 + Polling 재시도
│   ├── event/              # 이벤트 타입/페이로드 (6종: Created, Deleted, Updated, Viewed, Liked, Comment 등)
│   ├── audit/              # 감사 로그 — AuditLoggingFilter + AuditClient
│   ├── security/           # Gateway 인증 연동 — X-User-Id/Email/Role 헤더 → SecurityContext
│   ├── snowflake/          # 분산 ID 생성기 (Twitter Snowflake)
│   ├── response/           # BaseResponse, BaseResponseStatus
│   ├── exception/          # GlobalExceptionHandler
│   ├── domain/             # BaseTimeEntity
│   └── dataserializer/     # 이벤트 직렬화/역직렬화
│
├── infra/                   # 인프라 모듈 (Kafka, Redis 설정)
│
└── service/                 # 서비스 모듈 (13개)
    ├── gateway/             # API Gateway (:8000) — WebFlux + Netty, JWT 중앙 검증
    ├── user/                # 사용자 관리 (:8001) — 인증, USER/COMPANY/ADMIN 역할
    ├── jobposting/          # 채용공고 (:8002) — CRUD, Outbox 이벤트 발행
    ├── jobposting-comment/  # 댓글 (:8003) — Outbox 이벤트 발행
    ├── jobposting-view/     # 조회수 (:8004) — Outbox 이벤트 발행
    ├── jobposting-like/     # 좋아요 (:8005) — Outbox 이벤트 발행
    ├── jobposting-hot/      # 인기 공고 (:8006) — @Scheduled 배치 집계, Kafka Consumer
    ├── jobposting-read/     # 상세 조회 (:8007) — Circuit Breaker (5 CB), CQRS 로컬 캐시
    ├── resume/              # 이력서 (:8008)
    ├── apply/               # 지원서 + State Machine (:8009) — Enum 기반 상태 전이
    ├── notification/        # 알림 (:8010)
    ├── schedule/            # 면접 일정 (:8011)
    └── admin-audit/         # 감사 로그 (:8012) — 전체 API 요청 기록 조회
```

## 서비스별 적용 패턴

| 서비스 | 포트 | Outbox | CB | CQRS | 비고 |
|--------|------|--------|-----|------|------|
| gateway | 8000 | | | | JWT 중앙 검증, WebFlux |
| user | 8001 | | | | 인증, 역할 관리 |
| jobposting | 8002 | ✅ | | | 이벤트 발행 |
| jobposting-comment | 8003 | ✅ | | | 이벤트 발행 |
| jobposting-view | 8004 | ✅ | | | 이벤트 발행 |
| jobposting-like | 8005 | ✅ | | | 이벤트 발행 |
| jobposting-hot | 8006 | | | | @Scheduled 배치, Kafka Consumer |
| jobposting-read | 8007 | | ✅ (5개) | ✅ | Aggregator, ConcurrentHashMap 캐시 |
| resume | 8008 | | | | |
| apply | 8009 | | | | State Machine |
| notification | 8010 | | | | 알림 |
| schedule | 8011 | | | | 면접 일정 |
| admin-audit | 8012 | | | | 감사 로그 |

## 기술 스택

- Java 21 (LTS)
- Spring Boot 3.4.1
- Spring Cloud Gateway (WebFlux + Netty)
- Spring Data JPA
- Resilience4j (Circuit Breaker)
- PostgreSQL 18+ (서비스별 DB 분리)
- Redis Stack 7+ (Pub/Sub, Vector DB)
- Kafka 7.6 (Outbox Pattern 이벤트 스트리밍)
- Gradle 8.x (멀티 모듈 빌드)

## 실행

```bash
# 전체 빌드
./gradlew build

# 개별 서비스 실행 (Gateway 먼저)
./gradlew :service:gateway:bootRun
./gradlew :service:user:bootRun
./gradlew :service:jobposting:bootRun
# ...

# 테스트
./gradlew test
```

## 테스트

```bash
# 전체 테스트 (29 test files)
./gradlew test

# 특정 서비스 테스트
./gradlew :service:jobposting-read:test
```
