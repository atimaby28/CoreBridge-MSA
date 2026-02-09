package halo.corebridge.common.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventType {
    // Jobposting 이벤트
    JOBPOSTING_CREATED("corebridge-jobposting", JobpostingCreatedEventPayload.class),
    JOBPOSTING_UPDATED("corebridge-jobposting", JobpostingUpdatedEventPayload.class),
    JOBPOSTING_DELETED("corebridge-jobposting", JobpostingDeletedEventPayload.class),

    // Comment 이벤트
    COMMENT_CREATED("corebridge-comment", CommentCreatedEventPayload.class),
    COMMENT_DELETED("corebridge-comment", CommentDeletedEventPayload.class),

    // Like 이벤트
    JOBPOSTING_LIKED("corebridge-like", JobpostingLikedEventPayload.class),
    JOBPOSTING_UNLIKED("corebridge-like", JobpostingUnlikedEventPayload.class),

    // View 이벤트
    JOBPOSTING_VIEWED("corebridge-view", JobpostingViewedEventPayload.class),

    // Notification 이벤트
    NOTIFICATION_CREATED("corebridge-notification", NotificationCreatedEventPayload.class),
    ;

    private final String topic;
    private final Class<? extends EventPayload> payloadClass;
}
