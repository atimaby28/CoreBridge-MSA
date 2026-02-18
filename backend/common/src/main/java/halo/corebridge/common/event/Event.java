package halo.corebridge.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event<T extends EventPayload> {
    private String eventId;
    private EventType type;
    private Object payload;

    public static <T extends EventPayload> Event<T> of(EventType type, T payload) {
        Event<T> event = new Event<>();
        event.type = type;
        event.payload = payload;
        return event;
    }

    public static <T extends EventPayload> Event<T> of(String eventId, EventType type, T payload) {
        Event<T> event = new Event<>();
        event.eventId = eventId;
        event.type = type;
        event.payload = payload;
        return event;
    }
}