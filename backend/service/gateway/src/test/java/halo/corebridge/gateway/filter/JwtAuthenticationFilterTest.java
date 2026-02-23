package halo.corebridge.gateway.filter;

import halo.corebridge.gateway.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("JwtAuthenticationFilter 테스트")
class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;
    private GatewayFilterChain chain;
    private SecretKey secretKey;

    private static final String SECRET = "corebridge-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256";

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setAccessTokenExpiration(1800000);
        props.setRefreshTokenExpiration(604800000);

        filter = new JwtAuthenticationFilter(props);
        filter.init();

        chain = mock(GatewayFilterChain.class);
        given(chain.filter(any())).willReturn(Mono.empty());

        secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private String createToken(Long userId, String email, String role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1800000))
                .signWith(secretKey)
                .compact();
    }

    private String createExpiredToken() {
        return Jwts.builder()
                .subject("1001")
                .claim("email", "test@test.com")
                .claim("role", "ROLE_USER")
                .issuedAt(new Date(System.currentTimeMillis() - 3600000))
                .expiration(new Date(System.currentTimeMillis() - 1800000))
                .signWith(secretKey)
                .compact();
    }

    @Nested
    @DisplayName("공개 경로")
    class PublicPaths {

        @Test
        @DisplayName("로그인 경로는 토큰 없이 통과한다")
        void login_path_passes_without_token() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .post("/api/v1/users/login")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            filter.filter(exchange, chain).block();

            verify(chain).filter(exchange);
            assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("회원가입 경로는 토큰 없이 통과한다")
        void signup_path_passes_without_token() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .post("/api/v1/users/signup")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            filter.filter(exchange, chain).block();

            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("actuator 경로는 토큰 없이 통과한다")
        void actuator_path_passes_without_token() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/actuator/health")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            filter.filter(exchange, chain).block();

            verify(chain).filter(exchange);
        }
    }

    @Nested
    @DisplayName("Optional 인증 (GET 공개 경로)")
    class OptionalAuth {

        @Test
        @DisplayName("채용공고 GET은 토큰 없이 통과한다")
        void jobposting_list_get_passes_without_token() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/v1/jobpostings")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            filter.filter(exchange, chain).block();

            verify(chain).filter(any());
        }

        @Test
        @DisplayName("jobposting-read GET은 토큰 없이 통과한다")
        void jobposting_read_get_passes_without_token() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/v1/jobposting-read/123")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            filter.filter(exchange, chain).block();

            verify(chain).filter(any());
        }
    }

    @Nested
    @DisplayName("인증 필수 경로")
    class AuthRequired {

        @Test
        @DisplayName("토큰 없이 POST 요청하면 401 반환")
        void post_without_token_returns_401() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .post("/api/v1/jobpostings")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            filter.filter(exchange, chain).block();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("유효한 토큰으로 POST 요청하면 통과")
        void post_with_valid_token_passes() {
            String token = createToken(1001L, "company@test.com", "ROLE_COMPANY");

            MockServerHttpRequest request = MockServerHttpRequest
                    .post("/api/v1/jobpostings")
                    .cookie(new HttpCookie("accessToken", token))
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            filter.filter(exchange, chain).block();

            verify(chain).filter(any());
        }

        @Test
        @DisplayName("만료된 토큰으로 요청하면 401 반환")
        void expired_token_returns_401() {
            String expiredToken = createExpiredToken();

            MockServerHttpRequest request = MockServerHttpRequest
                    .post("/api/v1/jobpostings")
                    .cookie(new HttpCookie("accessToken", expiredToken))
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            filter.filter(exchange, chain).block();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("잘못된 토큰으로 요청하면 401 반환")
        void invalid_token_returns_401() {
            MockServerHttpRequest request = MockServerHttpRequest
                    .post("/api/v1/jobpostings")
                    .cookie(new HttpCookie("accessToken", "invalid.jwt.token"))
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            filter.filter(exchange, chain).block();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(chain, never()).filter(any());
        }
    }

    @Test
    @DisplayName("필터 순서는 -100 (가장 먼저 실행)")
    void filter_order_is_negative_100() {
        assertThat(filter.getOrder()).isEqualTo(-100);
    }
}
