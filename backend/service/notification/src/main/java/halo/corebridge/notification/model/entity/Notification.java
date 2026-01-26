package halo.corebridge.notification.model.entity;

import halo.corebridge.common.domain.BaseTimeEntity;
import halo.corebridge.notification.model.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user_id", columnList = "userId"),
        @Index(name = "idx_notification_user_read", columnList = "userId, isRead"),
        @Index(name = "idx_notification_created_at", columnList = "createdAt DESC")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(length = 500)
    private String link;

    @Column(nullable = false)
    @Builder.Default
    private boolean isRead = false;

    // 연관 데이터 (nullable)
    private Long relatedId;

    @Column(length = 50)
    private String relatedType;

    public void markAsRead() {
        this.isRead = true;
    }
}
