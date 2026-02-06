# CoreBridge Outbox Pattern 설계 문서

## 1. 개요

### 목적
서비스 간 데이터 일관성 보장을 위해 Outbox Pattern을 적용한다.
기존에는 jobposting-read/hot이 각 서비스에 직접 HTTP 호출로 통계를 조회했지만,
Outbox + Kafka 이벤트 기반으로 전환하여 **데이터 일관성 + 느슨한 결합**을 달성한다.

### 참조
- kuke-board (원본 Outbox 구현)
- CoreBridge-MSA (현재 프로젝트)

---

## 2. 아키텍처 변경 (Before → After)

### Before (현재)
```
[jobposting-read] --HTTP--> [jobposting] (공고 조회)
                  --HTTP--> [jobposting-view] (조회수)
                  --HTTP--> [jobposting-like] (좋아요수)
                  --HTTP--> [jobposting-comment] (댓글수)
                  --HTTP--> [user] (닉네임)

[jobposting-hot]  --HTTP--> [jobposting] (공고 목록)
                  --HTTP--> [jobposting-view/like/comment] (통계)
                  → @Scheduled로 매시간 폴링
```
**문제점**: HTTP 호출 실패 시 데이터 불일치, 높은 결합도, 폴링 비효율

### After (Outbox 적용)
```
[jobposting]         → Outbox → Kafka → [jobposting-read] (읽기 모델 업데이트)
                                       → [jobposting-hot]  (스코어 업데이트)
[jobposting-comment] → Outbox → Kafka → [jobposting-read]
                                       → [jobposting-hot]
[jobposting-like]    → Outbox → Kafka → [jobposting-read]
                                       → [jobposting-hot]
[jobposting-view]    → Outbox → Kafka → [jobposting-read]
                                       → [jobposting-hot]
```

---

## 3. 모듈 구조

### 3-1. 신규 모듈 (common 하위)

#### `common/data-serializer`
Jackson ObjectMapper 유틸. Event JSON 직렬화/역직렬화.

| 파일 | 설명 |
|------|------|
| `DataSerializer.java` | serialize/deserialize 유틸 (kuke-board 동일) |
| `build.gradle` | jackson-databind, jackson-datatype-jsr310 |

#### `common/event`
이벤트 정의. 모든 서비스가 공유하는 이벤트 타입과 페이로드.

| 파일 | 설명 |
|------|------|
| `Event.java` | eventId + type + payload 래퍼 |
| `EventType.java` | enum. 이벤트 종류 + Kafka topic 매핑 |
| `EventPayload.java` | 마커 인터페이스 |
| `payload/JobpostingCreatedEventPayload.java` | 공고 생성 페이로드 |
| `payload/JobpostingUpdatedEventPayload.java` | 공고 수정 페이로드 |
| `payload/JobpostingDeletedEventPayload.java` | 공고 삭제 페이로드 |
| `payload/CommentCreatedEventPayload.java` | 댓글 생성 페이로드 |
| `payload/CommentDeletedEventPayload.java` | 댓글 삭제 페이로드 |
| `payload/JobpostingLikedEventPayload.java` | 좋아요 페이로드 |
| `payload/JobpostingUnlikedEventPayload.java` | 좋아요 취소 페이로드 |
| `payload/JobpostingViewedEventPayload.java` | 조회 페이로드 |
| `build.gradle` | data-serializer 의존 |

#### `common/outbox-message-relay`
Outbox 엔티티 + Kafka 전송 + Redis Coordinator.

| 파일 | 설명 |
|------|------|
| `Outbox.java` | 엔티티 (outboxId, eventType, payload, shardKey, createdAt) |
| `OutboxRepository.java` | JPA Repository (shard별 미전송 조회) |
| `OutboxEvent.java` | Spring ApplicationEvent 래퍼 |
| `OutboxEventPublisher.java` | 이벤트 발행 진입점 (서비스에서 호출) |
| `MessageRelay.java` | BEFORE_COMMIT: DB 저장, AFTER_COMMIT: Kafka 전송, @Scheduled: 미전송 폴링 |
| `MessageRelayConfig.java` | KafkaTemplate, ThreadPool, @EnableAsync/@EnableScheduling |
| `MessageRelayConstants.java` | SHARD_COUNT = 4 |
| `MessageRelayCoordinator.java` | Redis 기반 인스턴스 등록/샤드 분배 |
| `AssignedShard.java` | 샤드 할당 계산 |
| `build.gradle` | spring-data-jpa, spring-kafka, spring-data-redis, snowflake, event |

### 3-2. settings.gradle 추가
```gradle
include 'common:data-serializer'
include 'common:event'
include 'common:outbox-message-relay'
```

---

## 4. EventType 정의

```java
public enum EventType {
    // Jobposting 이벤트
    JOBPOSTING_CREATED(JobpostingCreatedEventPayload.class, Topic.COREBRIDGE_JOBPOSTING),
    JOBPOSTING_UPDATED(JobpostingUpdatedEventPayload.class, Topic.COREBRIDGE_JOBPOSTING),
    JOBPOSTING_DELETED(JobpostingDeletedEventPayload.class, Topic.COREBRIDGE_JOBPOSTING),

    // Comment 이벤트
    COMMENT_CREATED(CommentCreatedEventPayload.class, Topic.COREBRIDGE_COMMENT),
    COMMENT_DELETED(CommentDeletedEventPayload.class, Topic.COREBRIDGE_COMMENT),

    // Like 이벤트
    JOBPOSTING_LIKED(JobpostingLikedEventPayload.class, Topic.COREBRIDGE_LIKE),
    JOBPOSTING_UNLIKED(JobpostingUnlikedEventPayload.class, Topic.COREBRIDGE_LIKE),

    // View 이벤트
    JOBPOSTING_VIEWED(JobpostingViewedEventPayload.class, Topic.COREBRIDGE_VIEW);

    public static class Topic {
        public static final String COREBRIDGE_JOBPOSTING = "corebridge-jobposting";
        public static final String COREBRIDGE_COMMENT = "corebridge-comment";
        public static final String COREBRIDGE_LIKE = "corebridge-like";
        public static final String COREBRIDGE_VIEW = "corebridge-view";
    }
}
```

---

## 5. EventPayload 정의

### JobpostingCreatedEventPayload
```java
private Long jobpostingId;
private String title;
private String content;
private Long boardId;
private Long userId;
private String requiredSkills;   // JSON string
private String preferredSkills;  // JSON string
private LocalDateTime createdAt;
private LocalDateTime updatedAt;
```

### JobpostingUpdatedEventPayload
```java
private Long jobpostingId;
private String title;
private String content;
private Long boardId;
private Long userId;
private String requiredSkills;
private String preferredSkills;
private LocalDateTime createdAt;
private LocalDateTime updatedAt;
```

### JobpostingDeletedEventPayload
```java
private Long jobpostingId;
private Long boardId;
private Long userId;
```

### CommentCreatedEventPayload
```java
private Long commentId;
private String content;
private Long parentCommentId;
private Long jobpostingId;
private Long userId;
private LocalDateTime createdAt;
private Long jobpostingCommentCount;  // 해당 공고의 총 댓글 수
```

### CommentDeletedEventPayload
```java
private Long commentId;
private Long jobpostingId;
private Long userId;
private Long jobpostingCommentCount;
```

### JobpostingLikedEventPayload
```java
private Long jobpostingLikeId;
private Long jobpostingId;
private Long userId;
private LocalDateTime createdAt;
private Long jobpostingLikeCount;  // 해당 공고의 총 좋아요 수
```

### JobpostingUnlikedEventPayload
```java
private Long jobpostingId;
private Long userId;
private Long jobpostingLikeCount;
```

### JobpostingViewedEventPayload
```java
private Long jobpostingId;
private Long userId;
private Long jobpostingViewCount;  // 해당 공고의 총 조회수
```

---

## 6. 생산자 서비스 변경

### 6-1. jobposting 서비스 (JobpostingService)

**변경 사항**: OutboxEventPublisher 주입 후, create/update/delete에서 이벤트 발행

```java
// create() 마지막에 추가
outboxEventPublisher.publish(
    EventType.JOBPOSTING_CREATED,
    JobpostingCreatedEventPayload.builder()
        .jobpostingId(jobposting.getJobpostingId())
        .title(jobposting.getTitle())
        .content(jobposting.getContent())
        .boardId(jobposting.getBoardId())
        .userId(jobposting.getUserId())
        .requiredSkills(jobposting.getRequiredSkills())
        .preferredSkills(jobposting.getPreferredSkills())
        .createdAt(jobposting.getCreatedAt())
        .updatedAt(jobposting.getUpdatedAt())
        .build(),
    jobposting.getBoardId()  // shardKey
);
```

**build.gradle 추가**: `implementation project(':common:outbox-message-relay')`

### 6-2. jobposting-comment 서비스 (CommentService)

```java
// create() 마지막에 추가
outboxEventPublisher.publish(
    EventType.COMMENT_CREATED,
    CommentCreatedEventPayload.builder()
        .commentId(comment.getCommentId())
        .content(comment.getContent())
        .parentCommentId(comment.getParentCommentId())
        .jobpostingId(comment.getJobpostingId())
        .userId(comment.getUserId())
        .createdAt(comment.getCreatedAt())
        .jobpostingCommentCount(commentRepository.countByJobpostingId(comment.getJobpostingId()))
        .build(),
    comment.getJobpostingId()
);
```

### 6-3. jobposting-like 서비스 (JobpostingLikeService)

```java
// like() 마지막에 추가
outboxEventPublisher.publish(
    EventType.JOBPOSTING_LIKED,
    JobpostingLikedEventPayload.builder()
        .jobpostingLikeId(like.getJobpostingLikeId())
        .jobpostingId(jobpostingId)
        .userId(userId)
        .createdAt(like.getCreatedAt())
        .jobpostingLikeCount(count(jobpostingId))
        .build(),
    jobpostingId
);
```

### 6-4. jobposting-view 서비스 (JobpostingViewService)

```java
// increase() 마지막에 추가
outboxEventPublisher.publish(
    EventType.JOBPOSTING_VIEWED,
    JobpostingViewedEventPayload.builder()
        .jobpostingId(jobpostingId)
        .userId(userId)
        .jobpostingViewCount(count(jobpostingId))
        .build(),
    jobpostingId
);
```

---

## 7. 소비자 서비스 변경

### 7-1. jobposting-read 서비스

**새로 추가할 파일:**

| 파일 | 설명 |
|------|------|
| `config/KafkaConfig.java` | KafkaListener 설정 |
| `consumer/JobpostingReadEventConsumer.java` | @KafkaListener, 4개 토픽 구독 |
| `service/event/handler/EventHandler.java` | 핸들러 인터페이스 |
| `service/event/handler/JobpostingCreatedEventHandler.java` | 읽기 모델 생성 |
| `service/event/handler/JobpostingUpdatedEventHandler.java` | 읽기 모델 수정 |
| `service/event/handler/JobpostingDeletedEventHandler.java` | 읽기 모델 삭제 |
| `service/event/handler/CommentCreatedEventHandler.java` | 댓글 수 업데이트 |
| `service/event/handler/CommentDeletedEventHandler.java` | 댓글 수 업데이트 |
| `service/event/handler/JobpostingLikedEventHandler.java` | 좋아요 수 업데이트 |
| `service/event/handler/JobpostingUnlikedEventHandler.java` | 좋아요 수 업데이트 |

**핵심 변경**: 기존 HTTP 호출 기반 → 이벤트 기반으로 읽기 모델 유지.
단, 캐시 미스 시 fallback으로 기존 HTTP 호출 유지 (kuke-board의 fetch() 패턴).

**build.gradle 추가**:
```gradle
implementation 'org.springframework.kafka:spring-kafka'
implementation project(':common:event')
implementation project(':common:data-serializer')
```

### 7-2. jobposting-hot 서비스

**새로 추가할 파일:**

| 파일 | 설명 |
|------|------|
| `config/KafkaConfig.java` | KafkaListener 설정 |
| `consumer/HotJobpostingEventConsumer.java` | @KafkaListener, 4개 토픽 구독 |
| `service/event/handler/EventHandler.java` | 핸들러 인터페이스 |
| `service/event/handler/JobpostingCreatedEventHandler.java` | 새 공고 스코어 등록 |
| `service/event/handler/CommentCreatedEventHandler.java` | 댓글 이벤트 → 스코어 재계산 |
| `service/event/handler/JobpostingLikedEventHandler.java` | 좋아요 이벤트 → 스코어 재계산 |
| `service/event/handler/JobpostingViewedEventHandler.java` | 조회 이벤트 → 스코어 재계산 |

**핵심 변경**: 기존 @Scheduled 매시간 폴링 → 이벤트 기반 실시간 스코어 업데이트.
@Scheduled는 보조 수단으로 유지 (이벤트 유실 복구용).

---

## 8. Kafka Topic 설계

| Topic | 생산자 | 소비자 | 이벤트 |
|-------|--------|--------|--------|
| `corebridge-jobposting` | jobposting | read, hot | CREATED, UPDATED, DELETED |
| `corebridge-comment` | comment | read, hot | CREATED, DELETED |
| `corebridge-like` | like | read, hot | LIKED, UNLIKED |
| `corebridge-view` | view | read, hot | VIEWED |

---

## 9. 인프라 요구사항

### Kafka
- 기존 docker-compose 또는 K3s에 Kafka 추가 필요
- 4개 토픽 자동 생성 설정

### Redis
- MessageRelayCoordinator용 (인스턴스 등록/샤드 분배)
- 기존 Redis 인스턴스 공유 가능

### DB
- 각 생산자 서비스 DB에 `outbox` 테이블 추가
- DDL: `outbox_id BIGINT PK, event_type VARCHAR, payload TEXT, shard_key BIGINT, created_at TIMESTAMP`

---

## 10. 구현 순서

### Step 1: common 모듈 생성 (오늘)
1. `common/data-serializer` 모듈 생성
2. `common/event` 모듈 생성 (EventType, Payload 클래스들)
3. `common/outbox-message-relay` 모듈 생성

### Step 2: 생산자 서비스 수정 (오늘)
4. `jobposting` 서비스에 OutboxEventPublisher 적용
5. `jobposting-comment` 서비스에 적용
6. `jobposting-like` 서비스에 적용
7. `jobposting-view` 서비스에 적용

### Step 3: 소비자 서비스 수정 (오늘~내일)
8. `jobposting-read`에 KafkaListener + EventHandler 추가
9. `jobposting-hot`에 KafkaListener + EventHandler 추가

### Step 4: 인프라 & 테스트 (내일)
10. Kafka 설정 (docker-compose / K3s)
11. 통합 테스트
12. 포트폴리오 캡처용 로그/메트릭 확인

---

## 11. 파일 변경 요약

| 구분 | 모듈 | 신규 파일 수 | 수정 파일 수 |
|------|------|-------------|-------------|
| common | data-serializer | 2 (java+gradle) | 0 |
| common | event | 11 (java+gradle) | 0 |
| common | outbox-message-relay | 10 (java+gradle) | 0 |
| service | jobposting | 0 | 2 (Service+gradle) |
| service | jobposting-comment | 0 | 2 (Service+gradle) |
| service | jobposting-like | 0 | 2 (Service+gradle) |
| service | jobposting-view | 0 | 2 (Service+gradle) |
| service | jobposting-read | 10 | 2 (Service+gradle) |
| service | jobposting-hot | 8 | 2 (Service+gradle) |
| root | settings.gradle | 0 | 1 |
| **합계** | | **~41 신규** | **~13 수정** |
