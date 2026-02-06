package halo.corebridge.common.event;

public interface EventHandler<T extends EventPayload> {
    void handle(Event<T> event);
    boolean supports(EventType eventType);
}
