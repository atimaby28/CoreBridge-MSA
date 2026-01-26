package halo.corebridge.notification.model.dto;

import halo.corebridge.notification.model.entity.Notification;
import halo.corebridge.notification.model.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

public class NotificationDto {

    // ===== 요청 DTOs =====

    /**
     * 내부 서비스에서 알림 생성 요청
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        @NotNull(message = "사용자 ID는 필수입니다")
        private Long userId;

        @NotNull(message = "알림 타입은 필수입니다")
        private NotificationType type;

        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다")
        private String title;

        @NotBlank(message = "메시지는 필수입니다")
        @Size(max = 500, message = "메시지는 500자 이하여야 합니다")
        private String message;

        @Size(max = 500, message = "링크는 500자 이하여야 합니다")
        private String link;

        private Long relatedId;
        private String relatedType;

        public Notification toEntity() {
            return Notification.builder()
                    .userId(userId)
                    .type(type)
                    .title(title)
                    .message(message)
                    .link(link)
                    .relatedId(relatedId)
                    .relatedType(relatedType)
                    .isRead(false)
                    .build();
        }
    }

    // ===== 응답 DTOs =====

    /**
     * 알림 상세 응답
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private NotificationType type;
        private String typeDescription;
        private String title;
        private String message;
        private String link;
        private boolean isRead;
        private Long relatedId;
        private String relatedType;
        private LocalDateTime createdAt;

        public static Response from(Notification notification) {
            return Response.builder()
                    .id(notification.getId())
                    .type(notification.getType())
                    .typeDescription(notification.getType().getDescription())
                    .title(notification.getTitle())
                    .message(notification.getMessage())
                    .link(notification.getLink())
                    .isRead(notification.isRead())
                    .relatedId(notification.getRelatedId())
                    .relatedType(notification.getRelatedType())
                    .createdAt(notification.getCreatedAt())
                    .build();
        }
    }

    /**
     * 읽지 않은 알림 개수 응답
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UnreadCountResponse {
        private long count;

        public static UnreadCountResponse of(long count) {
            return new UnreadCountResponse(count);
        }
    }

    /**
     * 알림 생성 결과 응답 (내부 API용)
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateResponse {
        private Long id;
        private boolean success;
        private String message;

        public static CreateResponse success(Long id) {
            return CreateResponse.builder()
                    .id(id)
                    .success(true)
                    .message("알림이 생성되었습니다")
                    .build();
        }

        public static CreateResponse fail(String reason) {
            return CreateResponse.builder()
                    .success(false)
                    .message(reason)
                    .build();
        }
    }
}
