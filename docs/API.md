# CoreBridge API 명세

## Base URL

| 서비스 | URL |
|--------|-----|
| user | http://localhost:8001 |
| jobposting | http://localhost:8002 |
| application | http://localhost:8003 |
| process | http://localhost:8004 |
| schedule | http://localhost:8005 |
| notification | http://localhost:8006 |

## 공통 응답 형식

```json
{
  "isSuccess": true,
  "code": 200,
  "message": "성공",
  "result": { ... }
}
```

---

## User Service (8001)

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
  "role": "USER"  // USER, COMPANY, ADMIN
}
```

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

---

## Jobposting Service (8002)

### 채용공고 목록
```
GET /api/v1/jobpostings?page=0&size=10
```

### 채용공고 상세
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
  "deadline": "2026-03-01T23:59:59"
}
```

---

## Application Service (8003)

### 지원하기 (USER)
```
POST /api/v1/applications
```

**Request Body:**
```json
{
  "jobpostingId": 6530505289252864,
  "userId": 6530505289252865,
  "resumeId": 6530505289252866
}
```

### 내 지원 목록
```
GET /api/v1/applications/users/{userId}
```

### 공고별 지원자 목록 (COMPANY)
```
GET /api/v1/applications/jobpostings/{jobpostingId}
```

---

## Process Service (8004) ⭐

### 프로세스 생성
```
POST /api/v1/processes
```

**Request Body:**
```json
{
  "applicationId": 6530505289252864,
  "jobpostingId": 6530505289252865,
  "userId": 6530505289252866
}
```

### 상태 전이 (핵심!)
```
PATCH /api/v1/processes/{processId}/transition
```

**Request Body:**
```json
{
  "nextStep": "DOCUMENT_PASS",
  "changedBy": 6530505289252867,
  "reason": "서류 합격",
  "note": "경력 우수"
}
```

**ProcessStep 종류:**
- APPLIED, DOCUMENT_REVIEW
- DOCUMENT_PASS, DOCUMENT_FAIL
- CODING_TEST, CODING_PASS, CODING_FAIL
- INTERVIEW_1, INTERVIEW_1_PASS, INTERVIEW_1_FAIL
- INTERVIEW_2, INTERVIEW_2_PASS, INTERVIEW_2_FAIL
- FINAL_REVIEW, FINAL_PASS, FINAL_FAIL

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

---

## Schedule Service (8005)

### 일정 생성
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

## Notification Service (8006)

### 알림 생성
```
POST /api/v1/notifications
```

**Request Body:**
```json
{
  "userId": 6530505289252864,
  "type": "PROCESS_UPDATE",
  "title": "채용 진행 알림",
  "message": "서류 전형에 합격하셨습니다."
}
```

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

## 공통 헤더

모든 API 요청에 포함:

```
Authorization: Bearer {jwt-token}
X-User-Id: {userId}
X-User-Email: {userEmail}
Content-Type: application/json
```
