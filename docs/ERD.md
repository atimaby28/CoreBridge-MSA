# CoreBridge ERD

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
│ status          │  │    │ deadline        │  │ │  │ created_at      │  │
│ created_at      │  │    │ status          │  │ │  └─────────────────┘  │
└─────────────────┘  │    │ created_at      │  │ │                       │
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
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        recruitment_process                          │
├─────────────────────────────────────────────────────────────────────┤
│ process_id (PK)      │ application_id (FK)  │ jobposting_id (FK)    │
│ user_id (FK)         │ current_step         │ previous_step         │
│ step_changed_at      │ created_at           │ updated_at            │
└─────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         process_history                             │
├─────────────────────────────────────────────────────────────────────┤
│ history_id (PK)      │ process_id (FK)      │ application_id (FK)   │
│ from_step            │ to_step              │ changed_by            │
│ reason               │ note                 │ created_at            │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│interview_schedule│      │   notification  │       │    audit_log    │
├─────────────────┤       ├─────────────────┤       ├─────────────────┤
│ schedule_id (PK)│       │ notification_id │       │ audit_id (PK)   │
│ application_id  │       │ user_id (FK)    │       │ user_id         │
│ jobposting_id   │       │ type            │       │ user_email      │
│ user_id (FK)    │       │ title           │       │ event_type      │
│ type            │       │ message         │       │ http_method     │
│ scheduled_at    │       │ status          │       │ uri             │
│ location        │       │ link_url        │       │ request_body    │
│ status          │       │ read_at         │       │ response_status │
│ created_at      │       │ created_at      │       │ created_at      │
└─────────────────┘       └─────────────────┘       └─────────────────┘
```

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

### recruitment_process (핵심!)
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
