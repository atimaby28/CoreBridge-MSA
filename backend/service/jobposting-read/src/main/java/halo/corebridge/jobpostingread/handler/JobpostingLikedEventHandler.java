package halo.corebridge.jobpostingread.handler;

import halo.corebridge.common.event.Event;
import halo.corebridge.common.event.EventHandler;
import halo.corebridge.common.event.EventType;
import halo.corebridge.common.event.JobpostingLikedEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobpostingLikedEventHandler implements EventHandler<JobpostingLikedEventPayload> {

    private final JobpostingReadCache readCache;

    @Override
    public void handle(Event<JobpostingLikedEventPayload> event) {
        JobpostingLikedEventPayload payload = event.getPayload();
        readCache.updateLikeCount(payload.getJobpostingId(), payload.getLikeCount());
        log.info("[ReadHandler] JOBPOSTING_LIKED: jobpostingId={}, likeCount={}",
                payload.getJobpostingId(), payload.getLikeCount());
    }

    @Override
    public boolean supports(EventType eventType) {
        return EventType.JOBPOSTING_LIKED == eventType;
    }
}
