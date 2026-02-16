# MSA 아키텍처 설계 회고

## 왜 MSA를 선택했나?

### 도메인 분석

채용 시스템의 특성:
- **공고 조회**: 트래픽 높음, 읽기 위주 → CQRS 캐시 + Circuit Breaker로 최적화
- **지원 처리**: 트래픽 중간, 쓰기 위주 → State Machine으로 상태 일관성 보장
- **AI 분석**: CPU 집약적, 장시간 처리 → n8n 비동기로 분리
- **알림 발송**: 비동기 처리 필요 → Outbox + Kafka + SSE
- **감사 로그**: 모든 서비스에서 호출 → common 모듈로 공통화

→ 기능별 트래픽 패턴이 달라서 **독립적 스케일링**과 **장애 격리** 필요

### MSA vs Monolith 비교

| 항목 | MSA | Monolith |
|------|-----|----------|
| 확장성 | ✅ 서비스별 스케일링 | ❌ 전체 스케일링 |
| 배포 | ✅ 독립 배포 | ❌ 전체 배포 |
| 장애 격리 | ✅ CB로 서비스 단위 격리 | ❌ 전체 영향 |
| 복잡도 | ❌ 높음 (13개 서비스 관리) | ✅ 낮음 |
| 데이터 일관성 | ❌ Outbox + Kafka로 최종 일관성 | ✅ 단일 DB 트랜잭션 |

**MSA의 복잡도를 감수한 이유:**
채용 시스템에서 공고 조회(읽기 위주)와 AI 분석(CPU 집약적)의 리소스 특성이 극단적으로 다름.
Monolith에서는 Ollama 80초 처리가 공고 조회 스레드까지 잠식할 수 있음.
MSA로 분리하면 AI Pipeline을 Docker Compose로 독립 운영하여 리소스 경합을 원천 차단.

## 서비스 분리 기준

### 1. 도메인 경계 (DDD)

```
Core Domain (핵심)
├── apply (지원 + 채용 프로세스 State Machine)
├── jobposting (채용공고)
└── resume (이력서)

Supporting Domain (지원)
├── user (사용자 관리)
├── schedule (면접 일정)
└── notification (실시간 알림)

Generic Domain (일반)
├── comment, view, like (소셜 기능)
├── read, hot (조회 최적화 - CQRS)
└── admin-audit (감사 로그)
```

### 2. 트래픽 패턴별 분리

| 서비스 | 트래픽 패턴 | 스케일링 전략 |
|--------|-----------|-------------|
| jobposting-read | 높은 읽기 | CQRS 캐시로 해결, 필요시 수평 확장 |
| apply | 중간 쓰기 | Outbox로 비동기 처리 |
| notification | 이벤트 기반 | Kafka Consumer, SSE 연결 수에 따라 확장 |
| AI Pipeline | CPU 집약적 | K3s 외부 Docker Compose로 리소스 분리 |

## 서비스 간 통신 아키텍처

### 현재 구현 (최종 상태)

```
동기 통신:
  Frontend → API Gateway (JWT 중앙 검증) → 각 서비스
  jobposting-read → CircuitBreaker → 원본 서비스 (캐시 미스 Fallback)

비동기 통신 (Outbox + Kafka):
  jobposting/comment/view/like → Outbox → Kafka → jobposting-read/hot (CQRS)
  apply (상태변경) → Outbox → Kafka → notification → SSE (실시간 알림)
  apply → n8n Webhook → FastAPI + Ollama (AI 분석, 비동기)
```

### 발전 과정

| 단계 | 통신 방식 | 문제점 | 해결 |
|------|---------|--------|------|
| 초기 | REST 직접 호출 | 서비스 주소 노출, JWT 중복 | **API Gateway** 도입 |
| 2단계 | Gateway + REST | HTTP 장애 전파, 높은 결합도 | **Outbox + Kafka** 이벤트 기반 전환 |
| 3단계 | 이벤트 기반 | 캐시 미스 시 HTTP Fallback 필요 | **CircuitBreaker** 적용 |
| 최종 | 이벤트 + CB + CQRS | AI 80초 블로킹 | **n8n 비동기** 파이프라인 |

## 데이터 관리

### Database per Service

```
각 서비스가 독립된 데이터베이스 스키마 사용:
├── user_db         → user 서비스 전용
├── jobposting_db   → jobposting 서비스 전용 + outbox 테이블
├── comment_db      → comment 서비스 전용 + outbox 테이블
├── apply_db        → apply 서비스 전용 + outbox 테이블
└── ...
```

### 데이터 정합성 전략

| 패턴 | 적용 |
|------|------|
| Outbox Pattern | DB 트랜잭션 내 이벤트 저장 → Kafka 발행 보장 |
| Eventual Consistency | 읽기 모델(CQRS)은 이벤트로 비동기 수렴 |
| Batch 보정 | @Scheduled로 매시간 읽기 모델 정합성 검증 |
| State Machine | apply 서비스에서 Enum 기반 상태 전이 규칙 강제 |

## 트러블슈팅

### 문제 1: 서비스 간 순환 참조
```
초기: apply → user (사용자 정보 조회), user → apply (지원 통계 조회)
해결: 통계 API를 apply 서비스 내부에 배치, 단방향 의존으로 정리
```

### 문제 2: 분산 트랜잭션
```
지원 생성 시: application 생성 + process 생성이 원자적이어야 함
해결: apply 서비스에 application + process를 통합하여 단일 트랜잭션으로 처리
     알림은 Outbox → Kafka로 최종 일관성 보장 (알림 실패해도 지원은 유효)
```

## 배운 점

1. **MSA는 만능이 아니다** — 복잡도 증가는 현실. 도메인 분석 없이 무작정 분리하면 오히려 비효율.
2. **이벤트 기반이 답이었다** — HTTP 직접 호출에서 Outbox+Kafka로 전환하면서 결합도와 장애 전파 문제가 해결됨.
3. **점진적 전환이 중요** — REST → Gateway → Outbox → CQRS → CB 순서로 단계적으로 개선하면서 각 패턴의 필요성을 체감.
4. **모니터링 없이 운영 불가** — 13개 서비스의 상태를 Prometheus+Grafana 없이는 파악 불가능.
