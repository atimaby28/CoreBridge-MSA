# CoreBridge CQRS + Batch 설계 문서

## 1. 개요

### 목적
읽기/쓰기 모델을 분리(CQRS)하고, Batch 처리로 읽기 모델의 정합성을 보장한다.
Outbox 이벤트 기반 실시간 업데이트 + Batch 기반 주기적 보정의 이중 구조.

### 구현 출처
kuke-board의 readModel 패턴을 채용 공고 + 통계(view/like/comment) 집계에 적용.
ConcurrentHashMap 캐시 + Batch 보정 로직은 도메인 특성에 맞게 자체 설계.

### 구현 상태: ✅ 완료

---

## 2. 왜 CQRS인가? — 문제 정의

### Before: N+1 HTTP 호출 문제
```
[Frontend] → [jobposting-read] → 매 요청마다 HTTP로 5개 서비스 호출
                                   → jobposting (원본)     ~20ms
                                   → user (닉네임)          ~15ms
                                   → view (조회수)          ~15ms
                                   → like (좋아요수)        ~15ms
                                   → comment (댓글수)       ~15ms
                                   ─────────────────────────────
                                   총 응답시간: 120~300ms (네트워크 지연 포함)
```

**문제점:**
- 목록 20건 조회 시 최대 100회 HTTP 호출 (20 × 5)
- 1개 서비스 장애 시 전체 조회 실패
- 트래픽이 많아지면 downstream 서비스에 과부하

### After: 이벤트 기반 읽기 모델
```
쓰기: [jobposting/like/view/comment] → Outbox → Kafka → [jobposting-read] 캐시 업데이트
읽기: [Frontend] → [Gateway] → [jobposting-read] → ConcurrentHashMap (2~5ms)
```

**개선 효과:**
- HTTP 호출 **100% 제거** → 응답 2~5ms (98% 감소)
- 서비스 장애와 무관하게 캐시에서 즉시 응답

---

## 3. 왜 ConcurrentHashMap인가? (Redis Cache 대비)

| 항목 | Redis Cache | **ConcurrentHashMap** |
|------|-----------|----------------------|
| 네트워크 | Redis 호출 1회 (0.5~1ms) | **없음 (0ms)** |
| 장애 격리 | Redis 장애 시 캐시 미스 | **JVM 내부, 외부 의존 없음** |
| 데이터 공유 | 멀티 인스턴스 간 공유 가능 | 인스턴스별 독립 |
| 메모리 | 별도 Redis 서버 | JVM 힙 사용 |
| 일관성 | 중앙 캐시 (강한 일관성) | 각 인스턴스 독립 수렴 (최종 일관성) |

**선택 근거:**
- 현재 jobposting-read는 **단일 인스턴스** 운영 → 인스턴스 간 공유 불필요
- 스케일아웃 시에도 각 인스턴스가 **Kafka 이벤트를 독립 소비**하므로 모든 인스턴스의 캐시가 동일하게 수렴 (Eventual Consistency)
- Redis 장애 시에도 서비스가 독립적으로 동작 → 가용성 향상
- 채용 공고 수가 수천 건 수준이면 JVM 힙으로 충분 (약 수십 MB)

**스케일아웃 시 Redis 전환 기준:**
- 인스턴스 3대 이상 + 캐시 워밍업 시간이 비즈니스에 영향을 줄 때
- Redis 전환 시에도 EventHandler 인터페이스는 동일, Repository 구현만 교체

---

## 4. 읽기 모델 설계

### 4-1. JobpostingQueryModel (ConcurrentHashMap)

```java
@Getter @Builder
public class JobpostingQueryModel {
    private Long jobpostingId;
    private String title;
    private String content;
    private Long boardId;
    private Long userId;
    private String nickname;         // user 서비스에서 가져온 닉네임
    private String requiredSkills;
    private String preferredSkills;
    private Long viewCount;          // view 이벤트로 업데이트
    private Long likeCount;          // like 이벤트로 업데이트
    private Long commentCount;       // comment 이벤트로 업데이트
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 4-2. jobposting-hot 읽기 모델 (Redis Sorted Set)

```
hot:jobposting:daily:{yyyy-MM-dd}  — Sorted Set (member=jobpostingId, score=hotScore)
```

**스코어 계산식:**
```
hotScore = viewCount × 0.3 + likeCount × 2.0 + commentCount × 3.0 + timeDecay
```

가중치 설정 근거: 댓글(3.0)은 작성 비용이 가장 높아 관심도 지표로 가장 강한 신호, 좋아요(2.0)는 클릭 한 번, 조회(0.3)는 가장 쉬운 행동이므로 가중치를 낮게 설정.

---

## 5. 이벤트 기반 업데이트 (실시간)

### EventHandler 매핑

| 이벤트 | jobposting-read 동작 | jobposting-hot 동작 |
|--------|---------------------|---------------------|
| JOBPOSTING_CREATED | QueryModel 생성 | 새 공고 등록 + 초기 스코어 |
| JOBPOSTING_UPDATED | QueryModel 수정 | - |
| JOBPOSTING_DELETED | QueryModel 삭제 | 스코어 목록에서 제거 |
| COMMENT_CREATED | commentCount 증가 | 댓글수 + 스코어 재계산 |
| COMMENT_DELETED | commentCount 감소 | 댓글수 + 스코어 재계산 |
| JOBPOSTING_LIKED | likeCount 증가 | 좋아요수 + 스코어 재계산 |
| JOBPOSTING_UNLIKED | likeCount 감소 | 좋아요수 + 스코어 재계산 |
| JOBPOSTING_VIEWED | (HTTP 호출 유지) | 조회수 + 스코어 재계산 |

**viewCount를 이벤트로 업데이트하지 않는 이유:**
조회 이벤트는 트래픽이 가장 많아 Kafka 부하가 과도. jobposting-read에서는 실시간 HTTP 호출을 유지하되, CircuitBreaker로 보호.

---

## 6. Batch 보정 (주기적)

### 목적
이벤트 유실이나 서버 재시작 시 읽기 모델과 원본 데이터가 어긋날 수 있다.
Batch로 주기적 보정하여 **최종 일관성(Eventual Consistency)**을 보장한다.

```java
@Scheduled(cron = "0 0 * * * *")  // 매시간
public void hourlyScoreRecalculation() {
    // 전체 공고의 인기 스코어 재계산 (이벤트 기반 업데이트의 보정 역할)
}

@Scheduled(cron = "0 0 0 * * *")  // 매일 자정
public void dailyCleanup() {
    // 7일 이전 데이터 정리 + 새 날짜 초기화
}
```

---

## 7. 캐시 미스 전략 (Fallback)

```java
public JobpostingReadDto.Response read(Long jobpostingId) {
    // 1차: ConcurrentHashMap에서 조회
    JobpostingQueryModel queryModel = queryModelRepository.read(jobpostingId);

    if (queryModel == null) {
        // 2차: CircuitBreaker를 통해 원본 서비스에서 fetch → 캐시에 저장
        queryModel = fetchFromOrigin(jobpostingId);
    }

    // viewCount는 항상 실시간 조회 (CircuitBreaker 보호)
    Long viewCount = viewClient.count(jobpostingId);

    return JobpostingReadDto.Response.from(queryModel, viewCount);
}
```

---

## 8. 성능 비교

| 항목 | Before (HTTP 호출) | After (CQRS 캐시) |
|------|-------------------|-------------------|
| 단건 조회 응답시간 | 120~300ms | **2~5ms** |
| HTTP 호출 수 | 5회/요청 | **0회** (캐시 히트 시) |
| 서비스 장애 영향 | 전체 조회 실패 | Fallback으로 부분 응답 |
| 데이터 신선도 | 실시간 | 이벤트 기반 준실시간 (~수초) |

---

## 9. 파일 변경 요약

| 모듈 | 구분 | 파일 수 |
|------|------|--------|
| jobposting-read | EventHandler 7개 + Consumer + Config | 10 |
| jobposting-read | QueryModel, Repository | 3 |
| jobposting-hot | EventHandler 7개 + Consumer + Config | 10 |
| jobposting-hot | ScoreUpdater 수정 | 1 |
| **합계** | | **~24 신규/수정** |

> **참고**: Outbox(이벤트 발행) + CQRS(이벤트 소비)는 하나의 흐름이며, 구현 시 함께 작업합니다.
