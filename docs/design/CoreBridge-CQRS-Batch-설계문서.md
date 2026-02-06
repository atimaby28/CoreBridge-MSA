# CoreBridge CQRS + Batch 설계 문서

## 1. 개요

### 목적
읽기/쓰기 모델을 분리(CQRS)하고, Batch 처리로 읽기 모델의 정합성을 보장한다.
Outbox 이벤트 기반 실시간 업데이트 + Batch 기반 주기적 보정의 이중 구조.

### 현재 상태
- `jobposting-read`: BFF 패턴으로 공고+통계 통합 조회 (HTTP 호출 기반)
- `jobposting-hot`: @Scheduled로 매시간 인기 공고 갱신 (HTTP 폴링)
- 이미 읽기 모델 역할은 하고 있지만, **이벤트 기반이 아님**

---

## 2. 아키텍처 변경 (Before → After)

### Before (현재)
```
[Frontend] → [jobposting-read] → 매 요청마다 HTTP로 5개 서비스 호출
                                   → jobposting (원본)
                                   → user (닉네임)
                                   → view (조회수)
                                   → like (좋아요수)
                                   → comment (댓글수)
```
**문제점**: N+1 HTTP 호출, 높은 지연, 서비스 장애 시 전체 조회 실패

### After (CQRS + Outbox + Batch)
```
쓰기 경로:
  [Frontend] → [Gateway] → [jobposting/like/view/comment]
                              ↓ (Outbox → Kafka)
읽기 경로:                     ↓
  [Frontend] → [Gateway] → [jobposting-read] ← Redis/DB 읽기 모델
                           [jobposting-hot]  ← Redis 인기 스코어

보정 경로:
  [Batch @Scheduled] → 주기적으로 읽기 모델 정합성 검증 및 보정
```

---

## 3. 읽기 모델 설계

### 3-1. jobposting-read 읽기 모델 (JobpostingQueryModel)

Redis Hash에 저장하는 비정규화된 읽기 전용 모델:

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

**Redis 키 구조:**
- `jobposting:query:{jobpostingId}` — 개별 공고 QueryModel (Hash)
- `jobposting:list:board:{boardId}` — 게시판별 공고 ID 목록 (Sorted Set, score=jobpostingId)
- `jobposting:count:board:{boardId}` — 게시판별 공고 수 (String)

### 3-2. jobposting-hot 읽기 모델

Redis Sorted Set에 저장하는 인기 공고 스코어:

```
hot:jobposting:daily:{yyyy-MM-dd}  — Sorted Set (member=jobpostingId, score=hotScore)
hot:jobposting:view:{jobpostingId}    — 조회수 (String)
hot:jobposting:like:{jobpostingId}    — 좋아요수 (String)
hot:jobposting:comment:{jobpostingId} — 댓글수 (String)
hot:jobposting:created:{jobpostingId} — 생성시간 (String)
```

**스코어 계산식:**
```
hotScore = viewCount * 0.3 + likeCount * 2.0 + commentCount * 3.0 + timeDecay
```

---

## 4. 이벤트 기반 업데이트 (실시간)

### 4-1. jobposting-read EventHandler 매핑

| 이벤트 | 핸들러 | 동작 |
|--------|--------|------|
| JOBPOSTING_CREATED | JobpostingCreatedEventHandler | QueryModel 생성, ID 리스트 추가, 공고수 갱신 |
| JOBPOSTING_UPDATED | JobpostingUpdatedEventHandler | QueryModel 수정 |
| JOBPOSTING_DELETED | JobpostingDeletedEventHandler | QueryModel 삭제, ID 리스트 제거, 공고수 갱신 |
| COMMENT_CREATED | CommentCreatedEventHandler | commentCount 증가 |
| COMMENT_DELETED | CommentDeletedEventHandler | commentCount 감소 |
| JOBPOSTING_LIKED | JobpostingLikedEventHandler | likeCount 증가 |
| JOBPOSTING_UNLIKED | JobpostingUnlikedEventHandler | likeCount 감소 |

**viewCount는 이벤트로 업데이트하지 않음** — 트래픽이 너무 많아서 jobposting-read에서는 실시간 API 호출 유지 (kuke-board 동일 패턴)

### 4-2. jobposting-hot EventHandler 매핑

| 이벤트 | 핸들러 | 동작 |
|--------|--------|------|
| JOBPOSTING_CREATED | JobpostingCreatedEventHandler | 새 공고 등록 + 초기 스코어 |
| JOBPOSTING_VIEWED | JobpostingViewedEventHandler | 조회수 업데이트 + 스코어 재계산 |
| JOBPOSTING_LIKED | JobpostingLikedEventHandler | 좋아요수 업데이트 + 스코어 재계산 |
| JOBPOSTING_UNLIKED | JobpostingUnlikedEventHandler | 좋아요수 업데이트 + 스코어 재계산 |
| COMMENT_CREATED | CommentCreatedEventHandler | 댓글수 업데이트 + 스코어 재계산 |
| COMMENT_DELETED | CommentDeletedEventHandler | 댓글수 업데이트 + 스코어 재계산 |
| JOBPOSTING_DELETED | ArticleDeletedEventHandler | 스코어 목록에서 제거 |

---

## 5. Batch 보정 (주기적)

### 5-1. 목적
이벤트 유실이나 Redis 장애 시 읽기 모델과 원본 데이터가 어긋날 수 있다.
Batch로 주기적 보정하여 **최종 일관성(Eventual Consistency)**을 보장한다.

### 5-2. jobposting-hot Batch (기존 @Scheduled 개선)

```java
@Component
@RequiredArgsConstructor
public class HotJobpostingScheduler {

    private final HotJobpostingService hotJobpostingService;

    /**
     * 매시간 전체 공고의 인기 스코어 재계산
     * 이벤트 기반 실시간 업데이트의 보정 역할
     */
    @Scheduled(cron = "0 0 * * * *")  // 매시간
    public void hourlyScoreRecalculation() {
        log.info("[Batch] 시간별 인기 스코어 재계산 시작");
        int count = hotJobpostingService.recalculateAllScores();
        log.info("[Batch] 시간별 인기 스코어 재계산 완료: {}건", count);
    }

    /**
     * 매일 자정 전날 데이터 정리 + 새 날짜 초기화
     */
    @Scheduled(cron = "0 0 0 * * *")  // 매일 자정
    public void dailyCleanup() {
        log.info("[Batch] 일일 정리 시작");
        hotJobpostingService.cleanupOldData(7);  // 7일 이전 삭제
        log.info("[Batch] 일일 정리 완료");
    }
}
```

### 5-3. jobposting-read Batch (신규)

```java
@Component
@RequiredArgsConstructor
public class ReadModelSyncScheduler {

    /**
     * 매 10분마다 읽기 모델 정합성 검증
     * Redis의 통계 수치를 원본 서비스와 비교하여 보정
     */
    @Scheduled(fixedDelay = 600000)  // 10분
    public void syncReadModel() {
        // 최근 업데이트된 공고들의 통계를 원본과 비교
        // 차이가 있으면 Redis 읽기 모델 갱신
    }
}
```

---

## 6. 캐시 미스 전략 (Fallback)

읽기 모델(Redis)에 데이터가 없을 때의 처리:

```java
public JobpostingReadDto.Response read(Long jobpostingId) {
    // 1차: Redis 읽기 모델에서 조회
    JobpostingQueryModel queryModel = queryModelRepository.read(jobpostingId);

    if (queryModel == null) {
        // 2차: 원본 서비스에서 fetch → Redis에 캐싱
        queryModel = fetchFromOrigin(jobpostingId);
    }

    // viewCount는 항상 실시간 조회
    Long viewCount = viewClient.count(jobpostingId);

    return JobpostingReadDto.Response.from(queryModel, viewCount);
}
```

---

## 7. 모니터링 & 포트폴리오 캡처

### 캡처 시나리오
1. **Before**: 기존 HTTP 호출 방식에서 공고 목록 조회 응답시간 (N+1 문제)
2. **After**: CQRS 읽기 모델에서 즉시 응답 (단일 Redis 조회)
3. **Batch 로그**: 스케줄러 실행 로그 + 보정 건수

### Grafana 패널
- 읽기 API 응답시간 Before/After
- Redis 캐시 히트율
- Batch 실행 주기 및 보정 건수

---

## 8. 파일 변경 요약

| 모듈 | 구분 | 파일 수 |
|------|------|--------|
| jobposting-read | EventHandler 7개 신규 | 7 |
| jobposting-read | EventConsumer, KafkaConfig 신규 | 2 |
| jobposting-read | QueryModel, Repository 신규 | 3 |
| jobposting-read | ReadModelSyncScheduler 신규 | 1 |
| jobposting-read | Service 수정 (이벤트 기반 전환) | 1 |
| jobposting-read | build.gradle, application.yml 수정 | 2 |
| jobposting-hot | EventHandler 7개 신규 | 7 |
| jobposting-hot | EventConsumer, KafkaConfig 신규 | 2 |
| jobposting-hot | ScoreUpdater 수정 (이벤트 기반) | 1 |
| jobposting-hot | build.gradle, application.yml 수정 | 2 |
| **합계** | | **~28 신규/수정** |

> **참고**: Outbox 설계문서의 소비자 파일과 중복됩니다.
> Outbox(이벤트 발행) + CQRS(이벤트 소비)는 하나의 흐름이며, 구현 시 함께 작업합니다.
