package halo.corebridge.user.security;

import halo.corebridge.user.model.enums.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtProvider 테스트")
class JwtProviderTest {

    private JwtProvider jwtProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm");
        jwtProperties.setAccessTokenExpiration(1800000L); // 30분
        jwtProperties.setRefreshTokenExpiration(604800000L); // 7일

        jwtProvider = new JwtProvider(jwtProperties);
        jwtProvider.init();
    }

    @Nested
    @DisplayName("createAccessToken()")
    class CreateAccessTokenTest {

        @Test
        @DisplayName("Access Token을 생성할 수 있다")
        void createAccessToken_ReturnsValidToken() {
            // given
            Long userId = 1L;
            String email = "test@example.com";
            UserRole role = UserRole.ROLE_USER;

            // when
            String token = jwtProvider.createAccessToken(userId, email, role);

            // then
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT는 3개의 파트로 구성
        }

        @Test
        @DisplayName("생성된 Access Token에서 userId를 추출할 수 있다")
        void createAccessToken_ContainsUserId() {
            // given
            Long userId = 123L;
            String email = "test@example.com";
            UserRole role = UserRole.ROLE_USER;

            // when
            String token = jwtProvider.createAccessToken(userId, email, role);
            Long extractedUserId = jwtProvider.getUserId(token);

            // then
            assertThat(extractedUserId).isEqualTo(userId);
        }

        @Test
        @DisplayName("생성된 Access Token에서 email을 추출할 수 있다")
        void createAccessToken_ContainsEmail() {
            // given
            Long userId = 1L;
            String email = "user@example.com";
            UserRole role = UserRole.ROLE_USER;

            // when
            String token = jwtProvider.createAccessToken(userId, email, role);
            String extractedEmail = jwtProvider.getEmail(token);

            // then
            assertThat(extractedEmail).isEqualTo(email);
        }

        @Test
        @DisplayName("생성된 Access Token에서 role을 추출할 수 있다")
        void createAccessToken_ContainsRole() {
            // given
            Long userId = 1L;
            String email = "test@example.com";
            UserRole role = UserRole.ROLE_COMPANY;

            // when
            String token = jwtProvider.createAccessToken(userId, email, role);
            UserRole extractedRole = jwtProvider.getRole(token);

            // then
            assertThat(extractedRole).isEqualTo(UserRole.ROLE_COMPANY);
        }
    }

    @Nested
    @DisplayName("createRefreshToken()")
    class CreateRefreshTokenTest {

        @Test
        @DisplayName("Refresh Token을 생성할 수 있다")
        void createRefreshToken_ReturnsValidToken() {
            // given
            Long userId = 1L;

            // when
            String token = jwtProvider.createRefreshToken(userId);

            // then
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("생성된 Refresh Token에서 userId를 추출할 수 있다")
        void createRefreshToken_ContainsUserId() {
            // given
            Long userId = 456L;

            // when
            String token = jwtProvider.createRefreshToken(userId);
            Long extractedUserId = jwtProvider.getUserId(token);

            // then
            assertThat(extractedUserId).isEqualTo(userId);
        }
    }

    @Nested
    @DisplayName("validateToken()")
    class ValidateTokenTest {

        @Test
        @DisplayName("유효한 토큰은 true를 반환한다")
        void validateToken_WithValidToken_ReturnsTrue() {
            // given
            String token = jwtProvider.createAccessToken(1L, "test@example.com", UserRole.ROLE_USER);

            // when
            boolean isValid = jwtProvider.validateToken(token);

            // then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("잘못된 형식의 토큰은 false를 반환한다")
        void validateToken_WithMalformedToken_ReturnsFalse() {
            // given
            String malformedToken = "invalid.token.format";

            // when
            boolean isValid = jwtProvider.validateToken(malformedToken);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("빈 토큰은 false를 반환한다")
        void validateToken_WithEmptyToken_ReturnsFalse() {
            // when
            boolean isValid = jwtProvider.validateToken("");

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("null 토큰은 false를 반환한다")
        void validateToken_WithNullToken_ReturnsFalse() {
            // when
            boolean isValid = jwtProvider.validateToken(null);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("만료된 토큰은 false를 반환한다")
        void validateToken_WithExpiredToken_ReturnsFalse() {
            // given - 만료 시간을 1ms로 설정한 JwtProvider 생성
            JwtProperties shortExpiry = new JwtProperties();
            shortExpiry.setSecret("test-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm");
            shortExpiry.setAccessTokenExpiration(1L); // 1ms
            shortExpiry.setRefreshTokenExpiration(1L);

            JwtProvider shortExpiryProvider = new JwtProvider(shortExpiry);
            shortExpiryProvider.init();

            String token = shortExpiryProvider.createAccessToken(1L, "test@example.com", UserRole.ROLE_USER);

            // 토큰 만료 대기
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // when
            boolean isValid = shortExpiryProvider.validateToken(token);

            // then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("getClaims()")
    class GetClaimsTest {

        @Test
        @DisplayName("토큰에서 Claims를 추출할 수 있다")
        void getClaims_ReturnsAllClaims() {
            // given
            Long userId = 999L;
            String email = "claims@example.com";
            UserRole role = UserRole.ROLE_ADMIN;

            String token = jwtProvider.createAccessToken(userId, email, role);

            // when
            Claims claims = jwtProvider.getClaims(token);

            // then
            assertThat(claims.getSubject()).isEqualTo(String.valueOf(userId));
            assertThat(claims.get("email", String.class)).isEqualTo(email);
            assertThat(claims.get("role", String.class)).isEqualTo(role.name());
            assertThat(claims.getIssuedAt()).isNotNull();
            assertThat(claims.getExpiration()).isNotNull();
        }
    }

    @Nested
    @DisplayName("getRefreshTokenExpiration()")
    class GetRefreshTokenExpirationTest {

        @Test
        @DisplayName("Refresh Token 만료 시간을 반환한다")
        void getRefreshTokenExpiration_ReturnsConfiguredValue() {
            // when
            long expiration = jwtProvider.getRefreshTokenExpiration();

            // then
            assertThat(expiration).isEqualTo(604800000L); // 7일
        }
    }
}
