package halo.corebridge.jobpostingread.handler;

import halo.corebridge.common.event.Event;
import halo.corebridge.common.event.EventHandler;
import halo.corebridge.common.event.EventType;
import halo.corebridge.common.event.CommentDeletedEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentDeletedEventHandler implements EventHandler<CommentDeletedEventPayload> {

    private final JobpostingReadCache readCache;

    @Override
    public void handle(Event<CommentDeletedEventPayload> event) {
        CommentDeletedEventPayload payload = event.getPayload();
        readCache.decrementCommentCount(payload.getJobpostingId());
        log.info("[ReadHandler] COMMENT_DELETED: jobpostingId={}", payload.getJobpostingId());
    }

    @Override
    public boolean supports(EventType eventType) {
        return EventType.COMMENT_DELETED == eventType;
    }
}
