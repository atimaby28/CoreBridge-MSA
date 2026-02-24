package halo.corebridge.common.outboxmessagerelay;

import halo.corebridge.common.event.EventPayload;
import halo.corebridge.common.event.EventType;
import halo.corebridge.common.event.JobpostingCreatedEventPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxEventPublisher 테스트")
class OutboxEventPublisherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private OutboxEventPublisher outboxEventPublisher;

    @Test
    @DisplayName("성공: 이벤트 발행 시 OutboxEvent가 ApplicationEventPublisher로 전달된다")
    void publish_sendsOutboxEvent() {
        // given
        EventType eventType = EventType.JOBPOSTING_CREATED;
        JobpostingCreatedEventPayload payload = new JobpostingCreatedEventPayload();
        Long shardKey = 100L;

        // when
        outboxEventPublisher.publish(eventType, payload, shardKey);

        // then
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(applicationEventPublisher, times(1)).publishEvent(captor.capture());

        OutboxEvent captured = captor.getValue();
        assertThat(captured).isNotNull();
        assertThat(captured.getOutbox()).isNotNull();
        assertThat(captured.getOutbox().getEventType()).isEqualTo(EventType.JOBPOSTING_CREATED);
        assertThat(captured.getOutbox().getPayload()).isNotNull();
    }

    @Test
    @DisplayName("성공: shardKey가 SHARD_COUNT로 모듈러 연산되어 저장된다")
    void publish_shardKeyIsModuloOfShardCount() {
        // given
        EventType eventType = EventType.JOBPOSTING_CREATED;
        JobpostingCreatedEventPayload payload = new JobpostingCreatedEventPayload();
        Long shardKey = 7L; // 7 % 4 = 3

        // when
        outboxEventPublisher.publish(eventType, payload, shardKey);

        // then
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        Outbox outbox = captor.getValue().getOutbox();
        assertThat(outbox.getShardKey()).isEqualTo(7L % MessageRelayConstants.SHARD_COUNT);
    }

    @Test
    @DisplayName("성공: 서로 다른 이벤트 발행 시 각각 다른 eventId가 생성된다")
    void publish_generatesUniqueEventIds() {
        // given
        EventType eventType = EventType.JOBPOSTING_CREATED;
        JobpostingCreatedEventPayload payload = new JobpostingCreatedEventPayload();

        // when
        outboxEventPublisher.publish(eventType, payload, 1L);
        outboxEventPublisher.publish(eventType, payload, 2L);

        // then
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(applicationEventPublisher, times(2)).publishEvent(captor.capture());

        String payload1 = captor.getAllValues().get(0).getOutbox().getPayload();
        String payload2 = captor.getAllValues().get(1).getOutbox().getPayload();
        assertThat(payload1).isNotEqualTo(payload2); // eventId가 다르므로 payload도 다름
    }
}
