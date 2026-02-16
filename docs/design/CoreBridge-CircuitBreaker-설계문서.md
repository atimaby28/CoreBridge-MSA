# CoreBridge CircuitBreaker 설계 문서

## 1. 개요

### 목적
서비스 간 HTTP 호출 시 장애 전파를 방지하기 위해 CircuitBreaker 패턴을 적용한다.
의존 서비스 장애 시 즉시 Fallback 응답을 반환하여 전체 시스템 안정성을 확보한다.

### 구현 출처
kuke-board에 없는 패턴으로, Resilience4j 기반 **독립 설계 및 구현**.
5개 서비스별 독립 CB 인스턴스 + CircuitBreakerEventConfig 상태 모니터링 포함.

### 구현 상태: ✅ 완료

---

## 2. 왜 Resilience4j인가?

### Hystrix vs Resilience4j

| 항목 | Hystrix | Resilience4j |
|------|---------|-------------|
| 상태 | **유지보수 종료** (2018 Netflix deprecated) | 활발한 업데이트 |
| 스레드 모델 | 별도 스레드풀 격리 필수 (오버헤드 큼) | 세마포어 기반 (경량) |
| Spring Boot 3 | **미지원** | 공식 지원 (spring-boot3 모듈) |
| Micrometer 연동 | 별도 설정 | 자동 연동 → Prometheus/Grafana 즉시 사용 |
| 함수형 API | ❌ | ✅ (람다 + 데코레이터 패턴) |

**결론**: Spring Boot 3.4 + Java 21 환경에서 Hystrix는 선택지가 아님. Resilience4j가 유일한 현실적 대안.

### 왜 Retry/Timeout이 아닌 CircuitBreaker인가?

| 패턴 | 장애 감지 | 장애 시 동작 | 복구 감지 | 적합 상황 |
|------|---------|------------|---------|---------|
| Retry | ❌ (반복 시도) | 장애 서비스에 부하 가중 | ❌ | 일시적 네트워크 오류 |
| Timeout | △ (개별 요청) | 타임아웃까지 스레드 블로킹 | ❌ | 지연 방지 |
| **CircuitBreaker** | ✅ (실패율 추적) | **즉시 Fallback** (네트워크 호출 없음) | ✅ (HALF_OPEN) | **서비스 장애** |

jobposting-read는 5개 서비스를 동기 호출하므로, 1개 서비스 장애 시 Retry는 장애를 악화시킴.
CircuitBreaker로 장애 서비스를 빠르게 차단하고 Fallback(0L)으로 부분 응답을 반환하는 것이 사용자 경험에 유리.

---

## 3. 파라미터 설정 근거

```yaml
resilience4j:
  circuitbreaker:
    instances:
      jobpostingService:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
        minimumNumberOfCalls: 5
```

### slidingWindowSize = 10

| 값 | 장점 | 단점 |
|----|------|------|
| 5 | 빠른 반응 | 일시적 네트워크 지연에도 OPEN → **false positive** |
| **10** | 적절한 반응 속도 + 안정성 균형 | - |
| 50 | 노이즈에 강함 | 실제 장애에도 반응이 느림 |

채용 플랫폼은 초당 수천 TPS가 아닌 중간 트래픽 수준. 10개 윈도우가 적절한 표본.

### failureRateThreshold = 50%

10개 요청 중 5개 이상 실패하면 서비스 장애로 판단. 단일 타임아웃이나 일시적 오류는 무시하되, 지속적 장애는 빠르게 감지.

### waitDurationInOpenState = 10s

| 값 | 동작 |
|----|------|
| 1~3s | 장애 서비스에 너무 빨리 재시도 → 부하 가중 |
| **10s** | 일반적인 서비스 재시작/복구 시간을 고려한 설정 |
| 60s+ | 복구된 서비스를 오래 차단 → 불필요한 Fallback 지속 |

### 5개 서비스별 독립 인스턴스

```
jobpostingService, userService, viewService, likeService, commentService
```

**왜 공유하지 않고 독립인가?**
- viewService만 장애여도 모든 CB가 OPEN되면 정상인 jobposting/user 조회까지 차단됨
- 서비스별 독립 CB로 장애 격리 — view가 OPEN이어도 나머지 정상 조회 가능

---

## 4. 아키텍처 변경 (Before → After)

### Before
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

## 5. 적용 대상 및 Fallback 전략

| 호출자 | 대상 서비스 | Fallback |
|--------|------------|----------|
| jobposting-read | jobposting | 캐시 데이터 또는 null |
| jobposting-read | user | "알 수 없음" |
| jobposting-read | jobposting-view | 0L |
| jobposting-read | jobposting-like | 0L |
| jobposting-read | jobposting-comment | 0L |

**Fallback 설계 원칙:**
- 통계(view/like/comment)는 0으로 대체 → 공고 자체는 조회 가능 (부분 응답)
- 닉네임은 "알 수 없음"으로 대체 → UI에서 자연스럽게 표시
- 공고 원본이 null이면 해당 공고 조회 불가 (핵심 데이터이므로)

---

## 6. 구현 방식

```java
@Component
@RequiredArgsConstructor
public class ViewClient {
    private final RestTemplate restTemplate;

    @CircuitBreaker(name = "viewService", fallbackMethod = "countFallback")
    public Long count(Long jobpostingId) {
        return restTemplate.getForObject(
            "http://corebridge-jobposting-view:8004/api/v1/jobposting-views/" + jobpostingId + "/count",
            Long.class
        );
    }

    private Long countFallback(Long jobpostingId, Throwable t) {
        log.warn("[CB] viewService fallback: jobpostingId={}, error={}", jobpostingId, t.getMessage());
        return 0L;
    }
}
```

---

## 7. 모니터링 & 포트폴리오 캡처

### Prometheus 메트릭 (Resilience4j 자동 노출)
- `resilience4j_circuitbreaker_state` — 현재 상태 (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
- `resilience4j_circuitbreaker_failure_rate` — 실패율
- `resilience4j_circuitbreaker_calls_seconds` — 호출 시간

### 캡처 시나리오 (5단계)
1. 정상 상태: 모든 CB CLOSED, 서비스 정상 응답
2. 장애 주입: jobposting 서비스 강제 중지 → 실패율 상승
3. OPEN 전환: failureRate > 50% → Fallback 즉시 응답 확인
4. HALF_OPEN: 10초 후 3개 시도 → 아직 장애면 다시 OPEN
5. 복구: 서비스 재시작 → HALF_OPEN → CLOSED 전환 확인

---

## 8. 파일 변경 요약

| 모듈 | 변경 | 파일 |
|------|------|------|
| jobposting-read | build.gradle에 resilience4j 추가 | 1 |
| jobposting-read | application.yml CB 설정 (5개 인스턴스) | 1 |
| jobposting-read | Client 5개에 @CircuitBreaker + fallback | 5 |
| jobposting-read | CircuitBreakerEventConfig 상태 모니터링 | 1 |
| **합계** | | **~8 수정** |
