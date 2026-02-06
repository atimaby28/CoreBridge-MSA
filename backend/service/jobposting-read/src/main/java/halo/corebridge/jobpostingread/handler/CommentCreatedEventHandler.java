package halo.corebridge.jobpostingread.handler;

import halo.corebridge.common.event.Event;
import halo.corebridge.common.event.EventHandler;
import halo.corebridge.common.event.EventType;
import halo.corebridge.common.event.CommentCreatedEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentCreatedEventHandler implements EventHandler<CommentCreatedEventPayload> {

    private final JobpostingReadCache readCache;

    @Override
    public void handle(Event<CommentCreatedEventPayload> event) {
        CommentCreatedEventPayload payload = (CommentCreatedEventPayload) event.getPayload();
        readCache.incrementCommentCount(payload.getJobpostingId());
        log.info("[ReadHandler] COMMENT_CREATED: jobpostingId={}", payload.getJobpostingId());
    }

    @Override
    public boolean supports(EventType eventType) {
        return EventType.COMMENT_CREATED == eventType;
    }
}
