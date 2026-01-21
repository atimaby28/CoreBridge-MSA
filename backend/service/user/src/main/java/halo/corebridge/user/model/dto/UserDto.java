package halo.corebridge.user.model.dto;

import halo.corebridge.user.model.entity.User;
import halo.corebridge.user.model.enums.UserRole;
import halo.corebridge.user.model.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class UserDto {

    // ============================================
    // Request DTOs
    // ============================================

    @Getter
    public static class SignupRequest {
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "이메일 형식이 올바르지 않습니다")
        private String email;

        @NotBlank(message = "닉네임은 필수입니다")
        @Size(min = 2, max = 20, message = "닉네임은 2~20자 이내입니다")
        private String nickname;

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, max = 20, message = "비밀번호는 8~20자 이내입니다")
        private String password;

        private UserRole role; // ROLE_USER 또는 ROLE_COMPANY만 허용 (Service에서 검증)

        /**
         * 허용된 역할인지 검증 (ADMIN 가입 방지)
         */
        public boolean isAllowedRole() {
            return role == null || role == UserRole.ROLE_USER || role == UserRole.ROLE_COMPANY;
        }
    }

    @Getter
    public static class LoginRequest {
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "이메일 형식이 올바르지 않습니다")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다")
        private String password;
    }

    @Getter
    public static class RefreshRequest {
        @NotBlank(message = "리프레시 토큰은 필수입니다")
        private String refreshToken;

        public RefreshRequest() {}

        public RefreshRequest(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    @Getter
    public static class UpdateRequest {
        @Size(min = 2, max = 20, message = "닉네임은 2~20자 이내입니다")
        private String nickname;

        @Size(min = 8, max = 20, message = "비밀번호는 8~20자 이내입니다")
        private String password;
    }

    @Getter
    public static class RoleUpdateRequest {
        private UserRole role;
    }

    @Getter
    public static class StatusUpdateRequest {
        private UserStatus status;
    }

    // ============================================
    // Response DTOs
    // ============================================

    @Getter
    @Builder
    public static class UserResponse {
        private Long userId;
        private String email;
        private String nickname;
        private UserRole role;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static UserResponse from(User user) {
            return UserResponse.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .role(user.getRole())
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class LoginResponse {
        private Long userId;
        private String email;
        private String nickname;
        private UserRole role;
        private String accessToken;
        private String refreshToken;

        public static LoginResponse of(User user, String accessToken, String refreshToken) {
            return LoginResponse.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .role(user.getRole())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        }

        /**
         * Cookie로 토큰 전달 시 응답에서 토큰 제거
         */
        public LoginResponse withoutTokens() {
            return LoginResponse.builder()
                    .userId(this.userId)
                    .email(this.email)
                    .nickname(this.nickname)
                    .role(this.role)
                    .accessToken(null)
                    .refreshToken(null)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;

        /**
         * Cookie로 토큰 전달 시 응답에서 토큰 제거
         */
        public TokenResponse withoutTokens() {
            return TokenResponse.builder()
                    .accessToken(null)
                    .refreshToken(null)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class AdminUserResponse {
        private Long userId;
        private String email;
        private String nickname;
        private UserRole role;
        private UserStatus status;
        private boolean enabled;
        private LocalDateTime lastLoginAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static AdminUserResponse from(User user) {
            return AdminUserResponse.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .role(user.getRole())
                    .status(user.getStatus())
                    .enabled(user.isEnabled())
                    .lastLoginAt(user.getLastLoginAt())
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class UserPageResponse {
        private List<AdminUserResponse> users;
        private Long totalCount;
        private Integer page;
        private Integer size;
        private Integer totalPages;
        private Boolean hasNext;
        private Boolean hasPrevious;

        public static UserPageResponse of(List<AdminUserResponse> users, Long totalCount,
                                          Integer page, Integer size) {
            int totalPages = (int) Math.ceil((double) totalCount / size);
            return UserPageResponse.builder()
                    .users(users)
                    .totalCount(totalCount)
                    .page(page)
                    .size(size)
                    .totalPages(totalPages)
                    .hasNext(page < totalPages - 1)
                    .hasPrevious(page > 0)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class UserStatsResponse {
        private Long totalUsers;
        private Long activeUsers;
        private Long blockedUsers;
        private Long adminCount;
        private Long companyCount;
        private Long userCount;
    }
}
