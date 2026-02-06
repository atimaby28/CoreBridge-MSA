package halo.corebridge.jobpostinghot.handler;

import halo.corebridge.common.event.*;
import halo.corebridge.jobpostinghot.model.entity.HotJobposting;
import halo.corebridge.jobpostinghot.model.entity.HotJobpostingId;
import halo.corebridge.jobpostinghot.repository.HotJobpostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

/**
 * 이벤트 수신 시 HotJobposting 테이블의 통계를 실시간 갱신.
 * 기존 @Scheduled 배치 + HTTP 호출 방식에서
 * → 이벤트 기반 실시간 갱신으로 전환.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotJobpostingEventHandler implements EventHandler<EventPayload> {

    private final HotJobpostingRepository hotJobpostingRepository;

    private static final Set<EventType> SUPPORTED_TYPES = Set.of(
            EventType.JOBPOSTING_CREATED,
            EventType.JOBPOSTING_UPDATED,
            EventType.JOBPOSTING_DELETED,
            EventType.JOBPOSTING_VIEWED,
            EventType.JOBPOSTING_LIKED,
            EventType.JOBPOSTING_UNLIKED,
            EventType.COMMENT_CREATED,
            EventType.COMMENT_DELETED
    );

    @Override
    @Transactional
    public void handle(Event<EventPayload> event) {
        EventType type = event.getType();
        EventPayload payload = (EventPayload) event.getPayload();

        switch (type) {
            case JOBPOSTING_CREATED -> handleCreated((JobpostingCreatedEventPayload) payload);
            case JOBPOSTING_UPDATED -> handleUpdated((JobpostingUpdatedEventPayload) payload);
            case JOBPOSTING_DELETED -> handleDeleted((JobpostingDeletedEventPayload) payload);
            case JOBPOSTING_VIEWED -> handleViewed((JobpostingViewedEventPayload) payload);
            case JOBPOSTING_LIKED -> handleLiked((JobpostingLikedEventPayload) payload);
            case JOBPOSTING_UNLIKED -> handleUnliked((JobpostingUnlikedEventPayload) payload);
            case COMMENT_CREATED -> handleCommentCreated((CommentCreatedEventPayload) payload);
            case COMMENT_DELETED -> handleCommentDeleted((CommentDeletedEventPayload) payload);
        }
    }

    @Override
    public boolean supports(EventType eventType) {
        return SUPPORTED_TYPES.contains(eventType);
    }

    private void handleCreated(JobpostingCreatedEventPayload payload) {
        LocalDate today = LocalDate.now();
        HotJobposting hot = HotJobposting.create(
                today, payload.getJobpostingId(), payload.getTitle(),
                payload.getBoardId(), 0L, 0L, 0L
        );
        hotJobpostingRepository.save(hot);
        log.info("[HotHandler] CREATED: jobpostingId={}", payload.getJobpostingId());
    }

    private void handleUpdated(JobpostingUpdatedEventPayload payload) {
        findAndUpdate(payload.getJobpostingId(), hot -> {
            // 제목 변경 반영 (필요하면 엔티티에 updateTitle 추가)
        });
        log.info("[HotHandler] UPDATED: jobpostingId={}", payload.getJobpostingId());
    }

    private void handleDeleted(JobpostingDeletedEventPayload payload) {
        LocalDate today = LocalDate.now();
        HotJobpostingId id = new HotJobpostingId(today, payload.getJobpostingId());
        hotJobpostingRepository.deleteById(id);
        log.info("[HotHandler] DELETED: jobpostingId={}", payload.getJobpostingId());
    }

    private void handleViewed(JobpostingViewedEventPayload payload) {
        findAndUpdate(payload.getJobpostingId(), hot ->
                hot.updateCounts(hot.getLikeCount(), hot.getCommentCount(), payload.getViewCount())
        );
        log.info("[HotHandler] VIEWED: jobpostingId={}, viewCount={}", payload.getJobpostingId(), payload.getViewCount());
    }

    private void handleLiked(JobpostingLikedEventPayload payload) {
        findAndUpdate(payload.getJobpostingId(), hot ->
                hot.updateCounts(payload.getLikeCount(), hot.getCommentCount(), hot.getViewCount())
        );
        log.info("[HotHandler] LIKED: jobpostingId={}, likeCount={}", payload.getJobpostingId(), payload.getLikeCount());
    }

    private void handleUnliked(JobpostingUnlikedEventPayload payload) {
        findAndUpdate(payload.getJobpostingId(), hot ->
                hot.updateCounts(payload.getLikeCount(), hot.getCommentCount(), hot.getViewCount())
        );
        log.info("[HotHandler] UNLIKED: jobpostingId={}, likeCount={}", payload.getJobpostingId(), payload.getLikeCount());
    }

    private void handleCommentCreated(CommentCreatedEventPayload payload) {
        findAndUpdate(payload.getJobpostingId(), hot ->
                hot.updateCounts(hot.getLikeCount(), hot.getCommentCount() + 1, hot.getViewCount())
        );
        log.info("[HotHandler] COMMENT_CREATED: jobpostingId={}", payload.getJobpostingId());
    }

    private void handleCommentDeleted(CommentDeletedEventPayload payload) {
        findAndUpdate(payload.getJobpostingId(), hot ->
                hot.updateCounts(hot.getLikeCount(), Math.max(0, hot.getCommentCount() - 1), hot.getViewCount())
        );
        log.info("[HotHandler] COMMENT_DELETED: jobpostingId={}", payload.getJobpostingId());
    }

    private void findAndUpdate(Long jobpostingId, java.util.function.Consumer<HotJobposting> updater) {
        LocalDate today = LocalDate.now();
        HotJobpostingId id = new HotJobpostingId(today, jobpostingId);
        hotJobpostingRepository.findById(id).ifPresent(hot -> {
            updater.accept(hot);
            hotJobpostingRepository.save(hot);
        });
    }
}
