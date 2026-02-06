# CoreBridge CircuitBreaker 설계 문서

## 1. 개요

### 목적
서비스 간 HTTP 호출 시 장애 전파를 방지하기 위해 CircuitBreaker 패턴을 적용한다.
의존 서비스 장애 시 즉시 Fallback 응답을 반환하여 전체 시스템 안정성을 확보한다.

### 기술 선택
- **Resilience4j** (독립 구현, kuke-board에 없음)
- Spring Boot Actuator + Prometheus 메트릭 노출 → Grafana 시각화

---

## 2. 아키텍처 변경 (Before → After)

### Before (현재)
```
[jobposting-read] --HTTP--> [jobposting] (장애 시 → 타임아웃 대기 → 스레드 고갈)
                  --HTTP--> [user] (장애 시 → 연쇄 실패)
                  --HTTP--> [jobposting-view] (장애 시 → 전체 조회 실패)
```
**문제점**: 하나의 서비스 장애가 호출자까지 전파 (Cascading Failure)

### After (CircuitBreaker 적용)
```
[jobposting-read] --CB--> [jobposting] (장애 시 → OPEN → Fallback 즉시 응답)
                  --CB--> [user] (장애 시 → Fallback: "알 수 없음")
                  --CB--> [jobposting-view] (장애 시 → Fallback: 0)
```
**상태 전이**: CLOSED → OPEN → HALF_OPEN → CLOSED

---

## 3. 적용 대상

### 3-1. 서비스 간 HTTP 호출 지점

| 호출자 | 대상 서비스 | 용도 | Fallback |
|--------|------------|------|----------|
| jobposting-read | jobposting | 공고 원본 조회 | 캐시 데이터 또는 빈 응답 |
| jobposting-read | user | 닉네임 조회 | "알 수 없음" |
| jobposting-read | jobposting-view | 조회수 | 0L |
| jobposting-read | jobposting-like | 좋아요수 | 0L |
| jobposting-read | jobposting-comment | 댓글수 | 0L |
| jobposting-hot | jobposting | 공고 목록 | 빈 리스트 |
| jobposting-hot | jobposting-view | 조회수 | 0L |
| jobposting-hot | jobposting-like | 좋아요수 | 0L |
| jobposting-hot | jobposting-comment | 댓글수 | 0L |
| apply | resume | 이력서 조회 | null (지원 차단) |
| jobposting (AI) | FastAPI | AI 벡터 저장 | 로그 경고 (비동기이므로) |

### 3-2. 우선순위
1. **jobposting-read** — 사용자 조회 경로, 가장 영향 큼
2. **jobposting-hot** — 인기 공고 조회
3. **apply** — 지원 프로세스

---

## 4. Resilience4j 설정

### 4-1. 의존성 추가 (해당 서비스 build.gradle)
```gradle
implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.2.0'
implementation 'org.springframework.boot:spring-boot-starter-aop'
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

### 4-2. application.yml 설정
```yaml
resilience4j:
  circuitbreaker:
    instances:
      jobpostingService:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10          # 최근 10개 요청 기준
        failureRateThreshold: 50       # 실패율 50% 이상 → OPEN
        waitDurationInOpenState: 10s   # OPEN 상태 10초 유지
        permittedNumberOfCallsInHalfOpenState: 3  # HALF_OPEN에서 3개 시도
        minimumNumberOfCalls: 5        # 최소 5개 요청 후 판단
      userService:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
        minimumNumberOfCalls: 5
      viewService:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
        minimumNumberOfCalls: 5
      likeService:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
        minimumNumberOfCalls: 5
      commentService:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
        minimumNumberOfCalls: 5

# Actuator에 CircuitBreaker 메트릭 노출
management:
  endpoints:
    web:
      exposure:
        include: health, prometheus, circuitbreakers
  metrics:
    tags:
      application: ${spring.application.name}
  health:
    circuitbreakers:
      enabled: true
```

---

## 5. 구현 방식

### 5-1. Client에 @CircuitBreaker 적용 (jobposting-read 예시)

```java
@Component
@RequiredArgsConstructor
public class JobpostingClient {
    private final RestTemplate restTemplate;

    @CircuitBreaker(name = "jobpostingService", fallbackMethod = "readFallback")
    public JobpostingResponse read(Long jobpostingId) {
        return restTemplate.getForObject(
            "http://corebridge-jobposting:8002/api/v1/jobpostings/" + jobpostingId,
            JobpostingResponse.class
        );
    }

    // Fallback: 장애 시 null 반환 → 호출자가 처리
    private JobpostingResponse readFallback(Long jobpostingId, Throwable t) {
        log.warn("[CircuitBreaker] jobposting 조회 실패, fallback: jobpostingId={}, error={}",
                jobpostingId, t.getMessage());
        return null;
    }
}
```

### 5-2. 각 Client Fallback 전략

| Client | 정상 응답 | Fallback 응답 |
|--------|----------|--------------|
| JobpostingClient.read() | JobpostingResponse | null |
| UserClient.getNickname() | "양승우" | "알 수 없음" |
| ViewClient.count() | 150L | 0L |
| LikeClient.count() | 23L | 0L |
| CommentClient.count() | 8L | 0L |

### 5-3. Service에서 Fallback 처리

```java
// JobpostingReadService
private JobpostingReadDto.Response toReadResponse(JobpostingClient.JobpostingResponse jobposting) {
    String nickname = userClient.getNickname(jobposting.getUserId());
    // nickname이 null이면 fallback이 작동한 것
    if (nickname == null) nickname = "알 수 없음";

    return JobpostingReadDto.Response.builder()
            .nickname(nickname)
            .viewCount(viewClient.count(jobpostingId))    // fallback: 0L
            .likeCount(likeClient.count(jobpostingId))    // fallback: 0L
            .commentCount(commentClient.count(jobpostingId)) // fallback: 0L
            .build();
}
```

---

## 6. 모니터링 & 포트폴리오 캡처

### 6-1. Prometheus 메트릭
Resilience4j가 자동으로 노출하는 메트릭:
- `resilience4j_circuitbreaker_state` — 현재 상태 (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
- `resilience4j_circuitbreaker_calls_seconds` — 호출 시간
- `resilience4j_circuitbreaker_failure_rate` — 실패율
- `resilience4j_circuitbreaker_buffered_calls` — 버퍼된 호출 수

### 6-2. Grafana 대시보드 패널 (캡처용)
1. **CircuitBreaker 상태 변화** — CLOSED→OPEN→HALF_OPEN→CLOSED 타임라인
2. **실패율 추이** — failureRateThreshold(50%) 라인과 함께
3. **응답시간 Before/After** — CB 없을 때 타임아웃 vs CB OPEN 시 Fallback 즉시 응답

### 6-3. 캡처 시나리오
1. 정상 상태에서 모든 서비스 동작 확인 (CLOSED)
2. jobposting 서비스 강제 중지 → 실패율 상승 → OPEN 전환 확인
3. OPEN 상태에서 Fallback 응답 즉시 반환 확인
4. waitDuration 후 HALF_OPEN → 복구 확인

---

## 7. 파일 변경 요약

| 모듈 | 변경 | 파일 |
|------|------|------|
| jobposting-read | build.gradle에 resilience4j 추가 | 1 |
| jobposting-read | application.yml CB 설정 추가 | 1 |
| jobposting-read | Client 5개에 @CircuitBreaker + fallback | 5 |
| jobposting-hot | build.gradle에 resilience4j 추가 | 1 |
| jobposting-hot | application.yml CB 설정 추가 | 1 |
| jobposting-hot | Client 4개에 @CircuitBreaker + fallback | 4 |
| **합계** | | **~13 수정** |
