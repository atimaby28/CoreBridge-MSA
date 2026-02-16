# CoreBridge API 명세

## Base URL

모든 API 요청은 **API Gateway (`:8000`)** 를 통해 접근합니다.

```
http://localhost:8000/api/v1/...
```

Gateway가 JWT를 중앙 검증한 후, Downstream 서비스로 `X-User-Id`, `X-User-Email`, `X-User-Role` 헤더를 전달합니다.

### 내부 서비스 포트 (개발/디버깅용)

| 포트 | 서비스 | 비고 |
|------|--------|------|
| 8000 | gateway | 모든 요청의 진입점 |
| 8001 | user | 사용자 인증/권한 |
| 8002 | jobposting | 채용공고 CRUD |
| 8003 | jobposting-comment | 댓글/답글 |
| 8004 | jobposting-view | 조회수 |
| 8005 | jobposting-like | 좋아요/찜 |
| 8006 | jobposting-hot | 인기 공고 |
| 8007 | jobposting-read | 공고 상세 (Aggregator) |
| 8008 | resume | 이력서 |
| 8009 | apply | 지원서 + 채용 프로세스 |
| 8010 | notification | 실시간 알림 |
| 8011 | schedule | 면접 일정 |
| 8012 | admin-audit | 감사 로그 |

---

## 공통 응답 형식

```json
{
  "isSuccess": true,
  "code": 200,
  "message": "성공",
  "result": { ... }
}
```

## 인증 방식

JWT 토큰은 **HttpOnly Cookie**로 전달됩니다.

```
Cookie: accessToken=eyJhbG...
```

Gateway에서 3단계로 인증을 처리합니다:

| 단계 | 경로 | 동작 |
|------|------|------|
| **완전 공개** | /signup, /login, /refresh, /actuator | 토큰 검사 스킵 |
| **Optional 인증** | GET /jobpostings, /comments 등 조회 API | 토큰 있으면 인증, 없으면 비인증 통과 |
| **인증 필수** | 나머지 모든 경로 | 토큰 없으면 401 Unauthorized |

---

## User Service

### 회원가입
```
POST /api/v1/users/signup
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동",
  "role": "USER"
}
```

> `role`: USER (구직자), COMPANY (기업), ADMIN (관리자)

### 로그인
```
POST /api/v1/users/login
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "userId": 6530505289252864,
  "email": "user@example.com",
  "name": "홍길동",
  "role": "USER",
  "token": "jwt-token..."
}
```

> 토큰은 `Set-Cookie: accessToken=...` HttpOnly Cookie로도 설정됩니다.

### 토큰 갱신
```
POST /api/v1/users/refresh
```

### 내 정보 조회
```
GET /api/v1/users/me
```

---

## Jobposting Service

### 채용공고 목록 (공개)
```
GET /api/v1/jobpostings?page=0&size=10
```

### 채용공고 상세 (공개)
```
GET /api/v1/jobpostings/{jobpostingId}
```

### 채용공고 등록 (COMPANY)
```
POST /api/v1/jobpostings
```

**Request Body:**
```json
{
  "title": "백엔드 개발자 채용",
  "companyName": "테크기업",
  "description": "...",
  "requirements": "Java, Spring Boot",
  "requiredSkills": "Java, Spring Boot, JPA",
  "preferredSkills": "Kafka, Redis, Kubernetes",
  "deadline": "2026-03-01T23:59:59"
}
```

### 채용공고 수정/삭제 (COMPANY)
```
PUT /api/v1/jobpostings/{jobpostingId}
DELETE /api/v1/jobpostings/{jobpostingId}
```

---

## Jobposting-Read Service (CQRS Aggregator)

### 공고 상세 (통계 포함)
```
GET /api/v1/jobposting-read/{jobpostingId}
```

**Response** (CQRS 캐시에서 조회, 2~5ms):
```json
{
  "jobpostingId": 6530505289252864,
  "title": "백엔드 개발자 채용",
  "nickname": "테크기업",
  "viewCount": 150,
  "likeCount": 23,
  "commentCount": 8,
  "createdAt": "2026-01-15T10:00:00"
}
```

### 공고 목록 (통계 포함)
```
GET /api/v1/jobposting-read?boardId=1&page=0&size=10
```

---

## Jobposting-Hot Service

### 인기 공고 목록
```
GET /api/v1/jobposting-hot?date=2026-02-15&limit=10
```

---

## Comment Service

### 댓글 목록 (공개)
```
GET /api/v1/comments/jobpostings/{jobpostingId}
```

### 댓글 작성 (인증 필수)
```
POST /api/v1/comments
```

**Request Body:**
```json
{
  "jobpostingId": 6530505289252864,
  "content": "좋은 공고네요!",
  "parentCommentId": null
}
```

### 댓글 삭제
```
DELETE /api/v1/comments/{commentId}
```

---

## View / Like Service

### 조회수 증가
```
POST /api/v1/jobposting-views/{jobpostingId}
```

### 좋아요 토글
```
POST /api/v1/jobposting-likes/{jobpostingId}
DELETE /api/v1/jobposting-likes/{jobpostingId}
```

### 좋아요 여부 확인
```
GET /api/v1/jobposting-likes/{jobpostingId}/check
```

---

## Resume Service

### 이력서 목록
```
GET /api/v1/resumes/users/{userId}
```

### 이력서 등록
```
POST /api/v1/resumes
```

**Request Body:**
```json
{
  "title": "2026 상반기 이력서",
  "content": "이력서 전문 텍스트..."
}
```

### 이력서 수정/삭제
```
PUT /api/v1/resumes/{resumeId}
DELETE /api/v1/resumes/{resumeId}
```

---

## Apply Service ⭐

지원서 관리와 채용 프로세스(State Machine)를 통합한 핵심 서비스.

### 지원하기 (USER)
```
POST /api/v1/applies
```

**Request Body:**
```json
{
  "jobpostingId": 6530505289252864,
  "resumeId": 6530505289252866
}
```

> 지원 시 자동으로 채용 프로세스 생성 (State: APPLIED)
> 비동기로 AI 이력서 분석 요청 (n8n Webhook)

### 내 지원 목록
```
GET /api/v1/applies/users/{userId}
```

### 공고별 지원자 목록 (COMPANY)
```
GET /api/v1/applies/jobpostings/{jobpostingId}
```

### 상태 전이 ⭐ (COMPANY)
```
PATCH /api/v1/processes/{processId}/transition
```

**Request Body:**
```json
{
  "nextStep": "DOCUMENT_PASS",
  "reason": "서류 합격",
  "note": "경력 우수"
}
```

**ProcessStep 전이 규칙:**
```
APPLIED → DOCUMENT_REVIEW
DOCUMENT_REVIEW → DOCUMENT_PASS / DOCUMENT_FAIL
DOCUMENT_PASS → CODING_TEST / INTERVIEW_1
CODING_TEST → CODING_PASS / CODING_FAIL
INTERVIEW_1 → INTERVIEW_1_PASS / INTERVIEW_1_FAIL
INTERVIEW_1_PASS → INTERVIEW_2
INTERVIEW_2 → INTERVIEW_2_PASS / INTERVIEW_2_FAIL
INTERVIEW_2_PASS → FINAL_REVIEW
FINAL_REVIEW → FINAL_PASS / FINAL_FAIL
```

> 상태 전이 시 Outbox → Kafka → notification으로 실시간 알림 자동 발송

### 프로세스 조회
```
GET /api/v1/processes/{processId}
GET /api/v1/processes/applications/{applicationId}
GET /api/v1/processes/jobpostings/{jobpostingId}
GET /api/v1/processes/users/{userId}
```

### 통계 API
```
GET /api/v1/processes/users/{userId}/stats
POST /api/v1/processes/company/stats
```

### AI 매칭 결과 조회
```
GET /api/v1/ai-matching/{applicationId}
```

---

## Schedule Service

### 일정 생성 (COMPANY)
```
POST /api/v1/schedules
```

**Request Body:**
```json
{
  "applicationId": 6530505289252864,
  "jobpostingId": 6530505289252865,
  "userId": 6530505289252866,
  "type": "INTERVIEW_1",
  "scheduledAt": "2026-02-15T14:00:00",
  "location": "본사 3층 회의실",
  "note": "포트폴리오 지참"
}
```

### 일정 조회
```
GET /api/v1/schedules/users/{userId}
GET /api/v1/schedules/jobpostings/{jobpostingId}
```

---

## Notification Service

### SSE 연결 (실시간 알림 수신)
```
GET /api/v1/notifications/subscribe
```

> EventSource로 연결, 15초 Heartbeat로 유지

### 알림 목록
```
GET /api/v1/notifications/users/{userId}
GET /api/v1/notifications/users/{userId}/unread
```

### 읽음 처리
```
POST /api/v1/notifications/{notificationId}/read
POST /api/v1/notifications/users/{userId}/read-all
```

---

## Admin-Audit Service (ADMIN)

### 감사 로그 조회
```
GET /api/v1/audit?page=0&size=20
```

### 사용자 관리
```
GET /api/v1/admin/users?page=0&size=20
PATCH /api/v1/admin/users/{userId}/status
```

### 사용자 통계
```
GET /api/v1/admin/stats
```
