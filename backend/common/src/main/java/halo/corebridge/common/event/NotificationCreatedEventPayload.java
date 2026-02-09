package halo.corebridge.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCreatedEventPayload implements EventPayload {
    private Long userId;
    private String type;          // NotificationType (DOCUMENT_PASS, FINAL_PASS 등)
    private String title;
    private String message;
    private String link;
    private Long relatedId;
    private String relatedType;
}
