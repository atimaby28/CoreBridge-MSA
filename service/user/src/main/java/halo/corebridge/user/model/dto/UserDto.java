package halo.corebridge.user.model.dto;

import halo.corebridge.user.model.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class UserDto {

    @Builder
    @Getter
    public static class UserCreateRequest {
        private String email;
        private String nickname;
        private String password;
    }

    @Builder
    @Getter
    public static class UserUpdateRequest {
        private String nickname;
        private String password;
    }

    @Builder
    @Getter
    public static class UserResponse {
        private Long userId;
        private String email;
        private String nickname;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static UserResponse from(User user) {
            return UserResponse.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .build();
        }
    }
}
