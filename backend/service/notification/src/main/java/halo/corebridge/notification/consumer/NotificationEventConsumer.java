package halo.corebridge.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import halo.corebridge.common.dataserializer.DataSerializer;
import halo.corebridge.common.event.Event;
import halo.corebridge.common.event.EventPayload;
import halo.corebridge.common.event.EventType;
import halo.corebridge.common.event.NotificationCreatedEventPayload;
import halo.corebridge.notification.model.dto.NotificationDto;
import halo.corebridge.notification.model.enums.NotificationType;
import halo.corebridge.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer — apply 서비스의 Outbox 이벤트 수신
 *
 * 흐름: apply(Outbox) → Kafka(corebridge-notification) → 여기서 수신
 *       → NotificationService.create() → DB 저장 + Redis Pub/Sub → SSE
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "corebridge-notification",
            groupId = "notification-group"
    )
    public void consume(String message) {
        log.info("[NotificationEventConsumer] 이벤트 수신");
        try {
            Event<EventPayload> event = DataSerializer.deserialize(message, Event.class);
            if (event == null || event.getType() != EventType.NOTIFICATION_CREATED) {
                log.warn("[NotificationEventConsumer] 지원하지 않는 이벤트: {}", event != null ? event.getType() : "null");
                return;
            }

            // 페이로드 역직렬화
            NotificationCreatedEventPayload payload = DataSerializer.deserialize(
                    event.getPayload(),
                    NotificationCreatedEventPayload.class
            );

            if (payload == null) {
                log.error("[NotificationEventConsumer] 페이로드 역직렬화 실패");
                return;
            }

            // NotificationService.create() 호출 → DB 저장 + Redis Pub/Sub → SSE
            NotificationDto.CreateRequest request = NotificationDto.CreateRequest.builder()
                    .userId(payload.getUserId())
                    .type(NotificationType.valueOf(payload.getType()))
                    .title(payload.getTitle())
                    .message(payload.getMessage())
                    .link(payload.getLink())
                    .relatedId(payload.getRelatedId())
                    .relatedType(payload.getRelatedType())
                    .build();

            NotificationDto.CreateResponse response = notificationService.create(request);

            if (response.isSuccess()) {
                log.info("[NotificationEventConsumer] 알림 생성 완료: userId={}, type={}",
                        payload.getUserId(), payload.getType());
            } else {
                log.warn("[NotificationEventConsumer] 알림 생성 실패: {}", response.getMessage());
            }

        } catch (Exception e) {
            log.error("[NotificationEventConsumer] 이벤트 처리 실패: {}", e.getMessage(), e);
        }
    }
}
