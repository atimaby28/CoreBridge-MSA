package halo.corebridge.jobpostingread.handler;

import halo.corebridge.common.event.Event;
import halo.corebridge.common.event.EventHandler;
import halo.corebridge.common.event.EventType;
import halo.corebridge.common.event.JobpostingUnlikedEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobpostingUnlikedEventHandler implements EventHandler<JobpostingUnlikedEventPayload> {

    private final JobpostingReadCache readCache;

    @Override
    public void handle(Event<JobpostingUnlikedEventPayload> event) {
        JobpostingUnlikedEventPayload payload = event.getPayload();
        readCache.updateLikeCount(payload.getJobpostingId(), payload.getLikeCount());
        log.info("[ReadHandler] JOBPOSTING_UNLIKED: jobpostingId={}, likeCount={}",
                payload.getJobpostingId(), payload.getLikeCount());
    }

    @Override
    public boolean supports(EventType eventType) {
        return EventType.JOBPOSTING_UNLIKED == eventType;
    }
}
