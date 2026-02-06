package halo.corebridge.common.outboxmessagerelay;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class OutboxEvent {
    private Outbox outbox;

    public static OutboxEvent of(Outbox outbox) {
        OutboxEvent event = new OutboxEvent();
        event.outbox = outbox;
        return event;
    }
}
