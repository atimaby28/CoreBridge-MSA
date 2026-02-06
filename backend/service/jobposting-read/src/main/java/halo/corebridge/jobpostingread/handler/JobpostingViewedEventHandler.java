package halo.corebridge.jobpostingread.handler;

import halo.corebridge.common.event.Event;
import halo.corebridge.common.event.EventHandler;
import halo.corebridge.common.event.EventType;
import halo.corebridge.common.event.JobpostingViewedEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobpostingViewedEventHandler implements EventHandler<JobpostingViewedEventPayload> {

    private final JobpostingReadCache readCache;

    @Override
    public void handle(Event<JobpostingViewedEventPayload> event) {
        JobpostingViewedEventPayload payload = event.getPayload();
        readCache.updateViewCount(payload.getJobpostingId(), payload.getViewCount());
        log.info("[ReadHandler] JOBPOSTING_VIEWED: jobpostingId={}, viewCount={}",
                payload.getJobpostingId(), payload.getViewCount());
    }

    @Override
    public boolean supports(EventType eventType) {
        return EventType.JOBPOSTING_VIEWED == eventType;
    }
}
