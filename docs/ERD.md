# CoreBridge ERD

## 서비스별 DB 분리 구조

MSA 원칙에 따라 각 서비스가 독립된 데이터베이스(스키마)를 소유합니다.
서비스 간 데이터 동기화는 Outbox → Kafka 이벤트로 처리합니다.

```
┌─── user-db ───┐   ┌─── jobposting-db ──┐   ┌─── resume-db ────┐
│    user        │   │    jobposting       │   │    resume         │
└────────────────┘   │    outbox           │   └──────────────────┘
                     └─────────────────────┘
                               │
┌─── comment-db ─┐   ┌─── view-db ────┐   ┌─── like-db ─────┐
│  comment        │   │  jobposting_   │   │  jobposting_    │
│  outbox         │   │  view          │   │  like           │
└─────────────────┘   │  outbox        │   │  outbox         │
                      └────────────────┘   └─────────────────┘

┌─── apply-db ──────────────────────┐   ┌─── notification-db ┐
│  application                       │   │  notification       │
│  recruitment_process               │   └─────────────────────┘
│  process_history                   │
│  ai_matching_result                │   ┌─── schedule-db ────┐
│  outbox                            │   │  interview_schedule │
└────────────────────────────────────┘   └─────────────────────┘

┌─── audit-db ──┐   ┌─── read (Redis/Memory) ──┐   ┌─── hot (Redis) ──┐
│  audit_log     │   │  CQRS 읽기 모델 (캐시)    │   │  인기 스코어      │
└────────────────┘   └──────────────────────────┘   └──────────────────┘
```

---

## 전체 ERD

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│      user       │       │   jobposting    │       │     resume      │
├─────────────────┤       ├─────────────────┤       ├─────────────────┤
│ user_id (PK)    │──┐    │ jobposting_id   │    ┌──│ resume_id (PK)  │
│ email           │  │    │ title           │    │  │ user_id (FK)    │──┐
│ password        │  │    │ company_name    │    │  │ title           │  │
│ name            │  │    │ writer_id (FK)  │──┐ │  │ content         │  │
│ role            │  │    │ description     │  │ │  │ status          │  │
│ status          │  │    │ required_skills │  │ │  │ created_at      │  │
│ created_at      │  │    │ preferred_skills│  │ │  └─────────────────┘  │
└─────────────────┘  │    │ deadline        │  │ │                       │
                     │    │ status          │  │ │                       │
                     │    │ created_at      │  │ │                       │
                     │    └─────────────────┘  │ │                       │
                     │             │           │ │                       │
                     │             ▼           │ │                       │
                     │    ┌─────────────────┐  │ │                       │
                     │    │   application   │  │ │                       │
                     │    ├─────────────────┤  │ │                       │
                     └───▶│ application_id  │◀─┘ │                       │
                          │ jobposting_id   │◀───┘                       │
                          │ user_id (FK)    │◀──────────────────────────┘
                          │ resume_id (FK)  │
                          │ status          │
                          │ memo            │
                          │ created_at      │
                          └─────────────────┘
                                   │
                     ┌─────────────┼─────────────┐
                     ▼                           ▼
┌────────────────────────────────┐  ┌────────────────────────────────┐
│     recruitment_process        │  │      ai_matching_result        │
├────────────────────────────────┤  ├────────────────────────────────┤
│ process_id (PK)                │  │ matching_id (PK)               │
│ application_id (FK)            │  │ application_id (FK)            │
│ jobposting_id (FK)             │  │ resume_summary                 │
│ user_id (FK)                   │  │ extracted_skills (JSON)        │
│ current_step                   │  │ similarity_score               │
│ previous_step                  │  │ total_score                    │
│ step_changed_at                │  │ grade                          │
│ created_at                     │  │ created_at                     │
└──────────────┬─────────────────┘  └────────────────────────────────┘
               │
               ▼
┌────────────────────────────────┐
│       process_history          │
├────────────────────────────────┤
│ history_id (PK)                │
│ process_id (FK)                │
│ application_id (FK)            │
│ from_step                      │
│ to_step                        │
│ changed_by                     │
│ reason                         │
│ note                           │
│ created_at                     │
└────────────────────────────────┘

┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│interview_schedule│  │   notification  │  │    audit_log    │
├─────────────────┤  ├─────────────────┤  ├─────────────────┤
│ schedule_id (PK)│  │ notification_id │  │ audit_id (PK)   │
│ application_id  │  │ user_id (FK)    │  │ user_id         │
│ jobposting_id   │  │ type            │  │ user_email      │
│ user_id (FK)    │  │ title           │  │ event_type      │
│ type            │  │ message         │  │ http_method     │
│ scheduled_at    │  │ status          │  │ uri             │
│ location        │  │ link_url        │  │ request_body    │
│ status          │  │ read_at         │  │ response_status │
│ created_at      │  │ created_at      │  │ created_at      │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

---

## Outbox 테이블 (4개 생산자 서비스 공통)

jobposting, jobposting-comment, jobposting-like, jobposting-view, apply 서비스의 DB에 각각 존재합니다.

```
┌────────────────────────────────────────────┐
│                  outbox                     │
├────────────────────────────────────────────┤
│ outbox_id (PK)    BIGINT   Snowflake ID    │
│ event_type        VARCHAR  EventType enum   │
│ payload           TEXT     JSON 직렬화       │
│ shard_key         BIGINT   파티셔닝 키       │
│ created_at        TIMESTAMP                 │
└────────────────────────────────────────────┘
```

**동작 방식:**
1. BEFORE_COMMIT: 비즈니스 데이터와 같은 트랜잭션으로 outbox INSERT
2. AFTER_COMMIT: 비동기로 Kafka 발행
3. 발행 성공 시 outbox 레코드 삭제
4. 실패 시 10초 Polling으로 재시도

---

## CQRS 읽기 모델 (jobposting-read)

ConcurrentHashMap 기반 인메모리 캐시. Kafka 이벤트로 실시간 업데이트.

```
┌────────────────────────────────────────────┐
│        JobpostingQueryModel (캐시)          │
├────────────────────────────────────────────┤
│ jobpostingId      Long                     │
│ title             String                   │
│ content           String                   │
│ boardId           Long                     │
│ userId            Long                     │
│ nickname          String     (user 서비스)  │
│ requiredSkills    String                   │
│ preferredSkills   String                   │
│ viewCount         Long    (view 이벤트)     │
│ likeCount         Long    (like 이벤트)     │
│ commentCount      Long    (comment 이벤트)  │
│ createdAt         LocalDateTime            │
│ updatedAt         LocalDateTime            │
└────────────────────────────────────────────┘
```

**캐시 미스 시**: CircuitBreaker를 통해 원본 서비스 HTTP 호출 (Fallback: 0L / null)

---

## 인기 공고 읽기 모델 (jobposting-hot, Redis)

```
hot:jobposting:daily:{yyyy-MM-dd}     Sorted Set (member=jobpostingId, score=hotScore)
hot:jobposting:view:{jobpostingId}     조회수 (String)
hot:jobposting:like:{jobpostingId}     좋아요수 (String)
hot:jobposting:comment:{jobpostingId}  댓글수 (String)
hot:jobposting:created:{jobpostingId}  생성시간 (String)
```

**스코어 계산식:**
```
hotScore = viewCount × 0.3 + likeCount × 2.0 + commentCount × 3.0 + timeDecay
```

---

## 테이블 상세

### user
| 컬럼 | 타입 | 설명 |
|------|------|------|
| user_id | BIGINT (PK) | Snowflake ID |
| email | VARCHAR(255) | 이메일 (UNIQUE) |
| password | VARCHAR(255) | BCrypt 암호화 |
| name | VARCHAR(100) | 이름 |
| role | ENUM | USER, COMPANY, ADMIN |
| status | ENUM | ACTIVE, INACTIVE, SUSPENDED |
| created_at | TIMESTAMP | 생성일시 |

### recruitment_process (핵심)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| process_id | BIGINT (PK) | Snowflake ID |
| application_id | BIGINT (FK) | 지원 ID |
| jobposting_id | BIGINT (FK) | 공고 ID |
| user_id | BIGINT (FK) | 지원자 ID |
| current_step | ENUM | 현재 단계 (ProcessStep) |
| previous_step | ENUM | 이전 단계 |
| step_changed_at | TIMESTAMP | 단계 변경 시각 |

### process_history
| 컬럼 | 타입 | 설명 |
|------|------|------|
| history_id | BIGINT (PK) | Snowflake ID |
| process_id | BIGINT (FK) | 프로세스 ID |
| from_step | ENUM | 변경 전 단계 |
| to_step | ENUM | 변경 후 단계 |
| changed_by | BIGINT | 변경한 사용자 ID |
| reason | VARCHAR(500) | 변경 사유 |
| note | TEXT | 메모 |

---

## ProcessStep ENUM 값

```sql
CREATE TYPE process_step AS ENUM (
  'APPLIED',
  'DOCUMENT_REVIEW',
  'DOCUMENT_PASS',
  'DOCUMENT_FAIL',
  'CODING_TEST',
  'CODING_PASS',
  'CODING_FAIL',
  'INTERVIEW_1',
  'INTERVIEW_1_PASS',
  'INTERVIEW_1_FAIL',
  'INTERVIEW_2',
  'INTERVIEW_2_PASS',
  'INTERVIEW_2_FAIL',
  'FINAL_REVIEW',
  'FINAL_PASS',
  'FINAL_FAIL'
);
```

### 상태 전이 다이어그램

```
APPLIED → DOCUMENT_REVIEW → DOCUMENT_PASS → INTERVIEW_1 → INTERVIEW_1_PASS
                │                    │              → INTERVIEW_2 → FINAL_REVIEW
                → DOCUMENT_FAIL      → CODING_TEST              → FINAL_PASS / FINAL_FAIL
```

Enum 기반 State Machine으로 허용된 전이만 가능하며, 모든 전이 이력이 process_history에 자동 기록됩니다.
