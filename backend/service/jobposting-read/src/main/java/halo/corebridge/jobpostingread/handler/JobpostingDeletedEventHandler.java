package halo.corebridge.jobpostingread.handler;

import halo.corebridge.common.event.Event;
import halo.corebridge.common.event.EventHandler;
import halo.corebridge.common.event.EventType;
import halo.corebridge.common.event.JobpostingDeletedEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobpostingDeletedEventHandler implements EventHandler<JobpostingDeletedEventPayload> {

    private final JobpostingReadCache readCache;

    @Override
    public void handle(Event<JobpostingDeletedEventPayload> event) {
        JobpostingDeletedEventPayload payload = event.getPayload();
        readCache.removeJobposting(payload.getJobpostingId());
        log.info("[ReadHandler] JOBPOSTING_DELETED: jobpostingId={}", payload.getJobpostingId());
    }

    @Override
    public boolean supports(EventType eventType) {
        return EventType.JOBPOSTING_DELETED == eventType;
    }
}
