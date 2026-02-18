package halo.corebridge.common.outboxmessagerelay;

import halo.corebridge.common.event.EventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Outbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private Long shardKey;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private int retryCount = 0;

    public static Outbox create(EventType eventType, String payload, Long shardKey) {
        Outbox outbox = new Outbox();
        outbox.eventType = eventType;
        outbox.payload = payload;
        outbox.shardKey = shardKey;
        outbox.createdAt = LocalDateTime.now();
        outbox.retryCount = 0;
        return outbox;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public boolean isRetryExhausted() {
        return this.retryCount >= MessageRelayConstants.MAX_RETRY_COUNT;
    }
}
