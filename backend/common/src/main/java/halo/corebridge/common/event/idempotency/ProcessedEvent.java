package halo.corebridge.common.event.idempotency;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_event", indexes = {
        @Index(name = "idx_processed_event_event_id", columnList = "eventId", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId;

    @Column(nullable = false)
    private String consumerGroup;

    @Column(nullable = false, updatable = false)
    private LocalDateTime processedAt;

    public static ProcessedEvent create(String eventId, String consumerGroup) {
        ProcessedEvent entity = new ProcessedEvent();
        entity.eventId = eventId;
        entity.consumerGroup = consumerGroup;
        entity.processedAt = LocalDateTime.now();
        return entity;
    }
}
