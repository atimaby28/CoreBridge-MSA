package halo.corebridge.user.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RefreshToken 엔티티 테스트")
class RefreshTokenTest {

    @Nested
    @DisplayName("RefreshToken.create() 정적 팩토리 메서드")
    class CreateTest {

        @Test
        @DisplayName("RefreshToken 생성 시 만료 시간이 올바르게 설정된다")
        void create_SetsExpirationCorrectly() {
            // given
            String token = "test-refresh-token";
            Long userId = 1L;
            long expirationMs = 604800000L; // 7일

            // when
            RefreshToken refreshToken = RefreshToken.create(token, userId, expirationMs);

            // then
            assertThat(refreshToken.getToken()).isEqualTo(token);
            assertThat(refreshToken.getUserId()).isEqualTo(userId);
            assertThat(refreshToken.getCreatedAt()).isNotNull();
            assertThat(refreshToken.getExpiresAt()).isNotNull();
            assertThat(refreshToken.getExpiresAt()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("만료 시간이 현재 시간 + expirationMs 근처이다")
        void create_ExpirationTimeIsAccurate() {
            // given
            String token = "test-token";
            Long userId = 1L;
            long expirationMs = 3600000L; // 1시간

            LocalDateTime beforeCreate = LocalDateTime.now();

            // when
            RefreshToken refreshToken = RefreshToken.create(token, userId, expirationMs);

            LocalDateTime afterCreate = LocalDateTime.now();

            // then
            // 만료 시간은 생성 시간 + 1시간 근처여야 함
            LocalDateTime expectedMin = beforeCreate.plusSeconds(expirationMs / 1000);
            LocalDateTime expectedMax = afterCreate.plusSeconds(expirationMs / 1000).plusSeconds(1);

            assertThat(refreshToken.getExpiresAt()).isAfterOrEqualTo(expectedMin.minusSeconds(1));
            assertThat(refreshToken.getExpiresAt()).isBeforeOrEqualTo(expectedMax);
        }
    }

    @Nested
    @DisplayName("RefreshToken 비즈니스 메서드")
    class BusinessMethodTest {

        @Test
        @DisplayName("isExpired()는 만료되지 않은 토큰에 대해 false를 반환한다")
        void isExpired_WithValidToken_ReturnsFalse() {
            // given
            RefreshToken refreshToken = RefreshToken.create(
                    "valid-token",
                    1L,
                    3600000L // 1시간 후 만료
            );

            // when & then
            assertThat(refreshToken.isExpired()).isFalse();
        }

        @Test
        @DisplayName("isExpired()는 이미 만료된 토큰에 대해 true를 반환한다")
        void isExpired_WithExpiredToken_ReturnsTrue() {
            // given
            RefreshToken refreshToken = RefreshToken.create(
                    "expired-token",
                    1L,
                    1L // 1ms 후 만료 (거의 즉시)
            );

            // 잠시 대기하여 토큰 만료
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // when & then
            assertThat(refreshToken.isExpired()).isTrue();
        }

        @Test
        @DisplayName("updateToken()으로 토큰과 만료 시간을 갱신할 수 있다")
        void updateToken_UpdatesTokenAndExpiration() {
            // given
            RefreshToken refreshToken = RefreshToken.create(
                    "old-token",
                    1L,
                    3600000L
            );
            String oldToken = refreshToken.getToken();
            LocalDateTime oldExpiresAt = refreshToken.getExpiresAt();

            String newToken = "new-token";
            long newExpirationMs = 7200000L; // 2시간

            // when
            refreshToken.updateToken(newToken, newExpirationMs);

            // then
            assertThat(refreshToken.getToken()).isEqualTo(newToken);
            assertThat(refreshToken.getToken()).isNotEqualTo(oldToken);
            assertThat(refreshToken.getExpiresAt()).isAfter(oldExpiresAt);
        }
    }
}
