package halo.corebridge.jobpostingread.consumer;

import halo.corebridge.common.dataserializer.DataSerializer;
import halo.corebridge.common.event.Event;
import halo.corebridge.common.event.EventHandler;
import halo.corebridge.common.event.EventPayload;
import halo.corebridge.common.event.EventType;
import halo.corebridge.common.event.idempotency.IdempotencyChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobpostingReadEventConsumer {

    private static final String CONSUMER_GROUP = "jobposting-read-group";
    private final List<EventHandler<?>> eventHandlers;
    private final IdempotencyChecker idempotencyChecker;

    @KafkaListener(
            topics = {"corebridge-jobposting", "corebridge-comment", "corebridge-like", "corebridge-view"},
            groupId = "jobposting-read-group",
            containerFactory = "jobpostingReadKafkaListenerContainerFactory"
    )
    public void consume(String message) {
        log.info("[JobpostingReadEventConsumer] received message");
        try {
            Event<EventPayload> event = DataSerializer.deserialize(message, Event.class);
            if (event == null) {
                log.error("[JobpostingReadEventConsumer] failed to deserialize message");
                return;
            }

            // 멱등성 체크: 이미 처리된 이벤트는 스킵
            if (idempotencyChecker.isDuplicate(event.getEventId(), CONSUMER_GROUP)) {
                log.info("[JobpostingReadEventConsumer] duplicate event skipped. eventId={}", event.getEventId());
                return;
            }

            EventType eventType = event.getType();
            // 페이로드를 실제 타입으로 역직렬화
            EventPayload payload = DataSerializer.deserialize(
                    event.getPayload(), eventType.getPayloadClass());
            Event<EventPayload> typedEvent = Event.of(event.getEventId(), eventType, payload);

            for (EventHandler handler : eventHandlers) {
                if (handler.supports(eventType)) {
                    handler.handle(typedEvent);
                }
            }

            // 처리 완료 기록
            idempotencyChecker.markAsProcessed(event.getEventId(), CONSUMER_GROUP);
        } catch (Exception e) {
            log.error("[JobpostingReadEventConsumer] error processing message", e);
        }
    }

    @Configuration
    static class KafkaConsumerConfig {
        @Value("${spring.kafka.bootstrap-servers}")
        private String bootstrapServers;

        @Bean
        public ConsumerFactory<String, String> jobpostingReadConsumerFactory() {
            Map<String, Object> props = new HashMap<>();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
            return new DefaultKafkaConsumerFactory<>(props);
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, String> jobpostingReadKafkaListenerContainerFactory() {
            ConcurrentKafkaListenerContainerFactory<String, String> factory =
                    new ConcurrentKafkaListenerContainerFactory<>();
            factory.setConsumerFactory(jobpostingReadConsumerFactory());
            factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
            return factory;
        }
    }
}
