package halo.corebridge.common.outboxmessagerelay;

import halo.corebridge.common.dataserializer.DataSerializer;
import halo.corebridge.common.event.Event;
import halo.corebridge.common.event.EventPayload;
import halo.corebridge.common.event.EventType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "outbox.enabled", havingValue = "true", matchIfMissing = false)
public class OutboxEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(EventType eventType, EventPayload payload, Long shardKey) {
        String eventId = UUID.randomUUID().toString();
        Outbox outbox = Outbox.create(
                eventType,
                DataSerializer.serialize(Event.of(eventId, eventType, payload)),
                shardKey % MessageRelayConstants.SHARD_COUNT
        );
        applicationEventPublisher.publishEvent(OutboxEvent.of(outbox));
    }
}
