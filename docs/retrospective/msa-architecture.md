# MSA 아키텍처 설계 회고

## 왜 MSA를 선택했나?

### 도메인 분석

채용 시스템의 특성:
- **공고 조회**: 트래픽 높음, 읽기 위주
- **지원 처리**: 트래픽 중간, 쓰기 위주
- **알림 발송**: 비동기 처리 필요
- **감사 로그**: 모든 서비스에서 호출

→ 기능별 트래픽 패턴이 달라서 **독립적 스케일링** 필요

### MSA vs Monolith 비교

| 항목 | MSA | Monolith |
|------|-----|----------|
| 확장성 | ✅ 서비스별 스케일링 | ❌ 전체 스케일링 |
| 배포 | ✅ 독립 배포 | ❌ 전체 배포 |
| 장애 격리 | ✅ 서비스 단위 | ❌ 전체 영향 |
| 복잡도 | ❌ 높음 | ✅ 낮음 |
| 데이터 일관성 | ❌ 분산 트랜잭션 | ✅ 단일 DB |

## 서비스 분리 기준

### 1. 도메인 경계 (DDD)

```
Core Domain (핵심)
├── process      # 채용 프로세스 상태관리
├── application  # 지원서 관리
└── jobposting   # 채용공고

Supporting Domain (지원)
├── user         # 사용자 관리
├── resume       # 이력서
├── schedule     # 면접 일정
└── notification # 알림

Generic Domain (일반)
├── like, view, comment  # 소셜 기능
├── read, hot            # 조회 최적화
└── audit                # 감사 로그
```

### 2. 변경 빈도

| 서비스 | 변경 빈도 | 이유 |
|--------|----------|------|
| process | 낮음 | 핵심 로직, 안정화 필요 |
| notification | 높음 | 알림 유형 자주 추가 |
| jobposting | 중간 | 공고 필드 추가 등 |

### 3. 팀 구조 (Conway's Law)

실제 프로젝트에서는 혼자 개발했지만, 팀 단위라면:
- 채용팀: process, application, schedule
- 공고팀: jobposting, like, view, comment
- 플랫폼팀: user, notification, audit

## 서비스 간 통신

### 동기 통신 (REST)

```
Frontend → 각 서비스 직접 호출
- 장점: 단순, 디버깅 쉬움
- 단점: 서비스 주소 노출

추후 개선: API Gateway 도입
```

### 비동기 고려 (미구현)

```
상태 변경 → 이벤트 발행 → 알림 서비스 구독
- 장점: 느슨한 결합
- 구현: Kafka, RabbitMQ 고려
```

## 데이터 관리

### Database per Service

```
각 서비스가 독립된 스키마 사용
├── user_db
├── jobposting_db
├── application_db
└── ...

현재는 단일 PostgreSQL, 스키마로 분리
```

### 데이터 정합성

```
Process 상태 변경 시:
1. Process 서비스: 상태 변경 + 이력 저장 (트랜잭션)
2. Notification 서비스: 알림 발송 (별도 호출)

→ 알림 실패해도 상태 변경은 롤백 안 함 (Eventually Consistent)
```

## 트러블슈팅

### 문제 1: 서비스 간 순환 참조

```
Application → Process 호출
Process → Application 호출 (통계용)

해결: 통계 API는 별도 분리, 단방향 의존
```

### 문제 2: 분산 트랜잭션

```
지원 생성 시:
1. Application 생성
2. Process 생성

해결: Saga 패턴 대신 보상 트랜잭션
- Process 생성 실패 시 → Application 삭제 API 호출
```

## 배운 점

1. **MSA는 만능이 아니다** - 복잡도 증가는 현실
2. **도메인 이해가 먼저** - 무작정 분리하면 망함
3. **점진적 전환** - 모놀리스 → MSA 단계적으로
4. **모니터링 필수** - 분산 시스템 디버깅은 어려움
