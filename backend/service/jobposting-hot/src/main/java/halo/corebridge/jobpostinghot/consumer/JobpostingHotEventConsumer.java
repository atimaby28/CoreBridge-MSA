package halo.corebridge.jobpostinghot.consumer;

import halo.corebridge.common.dataserializer.DataSerializer;
import halo.corebridge.common.event.Event;
import halo.corebridge.common.event.EventHandler;
import halo.corebridge.common.event.EventPayload;
import halo.corebridge.common.event.EventType;
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
public class JobpostingHotEventConsumer {

    private final List<EventHandler<?>> eventHandlers;

    @KafkaListener(
            topics = {"corebridge-jobposting", "corebridge-comment", "corebridge-like", "corebridge-view"},
            groupId = "jobposting-hot-group",
            containerFactory = "jobpostingHotKafkaListenerContainerFactory"
    )
    public void consume(String message) {
        log.info("[JobpostingHotEventConsumer] received message");
        try {
            Event<EventPayload> event = DataSerializer.deserialize(message, Event.class);
            if (event == null) {
                log.error("[JobpostingHotEventConsumer] failed to deserialize message");
                return;
            }

            EventType eventType = event.getType();
            EventPayload payload = DataSerializer.deserialize(
                    event.getPayload(), eventType.getPayloadClass());
            Event<EventPayload> typedEvent = Event.of(eventType, payload);

            for (EventHandler handler : eventHandlers) {
                if (handler.supports(eventType)) {
                    handler.handle(typedEvent);
                }
            }
        } catch (Exception e) {
            log.error("[JobpostingHotEventConsumer] error processing message", e);
        }
    }

    @Configuration
    static class KafkaConsumerConfig {
        @Value("${spring.kafka.bootstrap-servers}")
        private String bootstrapServers;

        @Bean
        public ConsumerFactory<String, String> jobpostingHotConsumerFactory() {
            Map<String, Object> props = new HashMap<>();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
            return new DefaultKafkaConsumerFactory<>(props);
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, String> jobpostingHotKafkaListenerContainerFactory() {
            ConcurrentKafkaListenerContainerFactory<String, String> factory =
                    new ConcurrentKafkaListenerContainerFactory<>();
            factory.setConsumerFactory(jobpostingHotConsumerFactory());
            factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
            return factory;
        }
    }
}
