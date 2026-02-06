package halo.corebridge.common.event;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Event<T extends EventPayload> {
    private EventType type;
    private T payload;

    public static <T extends EventPayload> Event<T> of(EventType type, T payload) {
        Event<T> event = new Event<>();
        event.type = type;
        event.payload = payload;
        return event;
    }
}
