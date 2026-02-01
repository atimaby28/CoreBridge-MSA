package halo.corebridge.gateway.filter;

import halo.corebridge.gateway.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Gateway JWT 인증 필터
 * - 모든 요청에서 JWT 검증
 * - 검증 성공 시 userId, email, role을 헤더에 추가하여 downstream 서비스로 전달
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String ACCESS_TOKEN_COOKIE = "accessToken";

    // 인증 없이 접근 가능한 경로
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/users/signup",
            "/api/v1/users/login",
            "/api/v1/users/refresh",
            "/actuator",
            "/health"
    );

    // GET 요청은 인증 없이 허용하는 경로
    private static final List<String> PUBLIC_GET_PATHS = List.of(
            "/api/v1/jobpostings",
            "/api/v1/jobposting-read",   // 통계 포함 조회
            "/api/v1/hot-jobpostings",   // 인기 공고 조회
            "/api/v1/comments",          // 댓글 조회
            "/api/v1/jobposting-views",  // 조회수 조회
            "/api/v1/jobposting-likes"   // 좋아요 수 조회
    );

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        // 1. 완전 공개 경로 (항상 인증 스킵)
        if (isFullyPublicPath(path)) {
            return chain.filter(exchange);
        }

        // 2. 토큰 추출
        String token = resolveToken(request);

        // 3. GET 공개 경로: 토큰 있으면 인증 시도, 없으면 그냥 통과 (Optional 인증)
        if (isOptionalAuthPath(path, method)) {
            if (token != null) {
                try {
                    Claims claims = validateAndGetClaims(token);
                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-User-Id", claims.getSubject())
                            .header("X-User-Email", claims.get("email", String.class))
                            .header("X-User-Role", claims.get("role", String.class))
                            .build();
                    log.debug("Optional 인증 성공: userId={}, path={}", claims.getSubject(), path);
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                } catch (JwtException e) {
                    // 토큰이 잘못되어도 공개 경로이므로 그냥 통과
                    log.debug("Optional 인증 실패 (무시하고 통과): path={}, reason={}", path, e.getMessage());
                }
            }
            return chain.filter(exchange);
        }

        // 4. 인증 필수 경로
        if (token == null) {
            return onError(exchange, "토큰이 없습니다", HttpStatus.UNAUTHORIZED);
        }

        // 5. 토큰 검증
        try {
            Claims claims = validateAndGetClaims(token);

            // 6. 검증 성공 → 헤더에 사용자 정보 추가
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Email", claims.get("email", String.class))
                    .header("X-User-Role", claims.get("role", String.class))
                    .build();

            log.debug("JWT 인증 성공: userId={}, path={}", claims.getSubject(), path);

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (ExpiredJwtException e) {
            log.warn("만료된 토큰: {}", e.getMessage());
            return onError(exchange, "토큰이 만료되었습니다", HttpStatus.UNAUTHORIZED);
        } catch (JwtException e) {
            log.warn("잘못된 토큰: {}", e.getMessage());
            return onError(exchange, "유효하지 않은 토큰입니다", HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public int getOrder() {
        return -100; // 가장 먼저 실행
    }

    /**
     * 완전 공개 경로인지 확인 (토큰 검사 자체를 하지 않음)
     */
    private boolean isFullyPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Optional 인증 경로인지 확인 (토큰 있으면 인증, 없으면 그냥 통과)
     */
    private boolean isOptionalAuthPath(String path, String method) {
        if ("GET".equals(method)) {
            for (String publicGetPath : PUBLIC_GET_PATHS) {
                if (path.startsWith(publicGetPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Cookie에서 Access Token 추출
     */
    private String resolveToken(ServerHttpRequest request) {
        HttpCookie cookie = request.getCookies().getFirst(ACCESS_TOKEN_COOKIE);
        return cookie != null ? cookie.getValue() : null;
    }

    /**
     * 토큰 검증 및 Claims 추출
     */
    private Claims validateAndGetClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 에러 응답
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        log.warn("인증 실패: {} - {}", status, message);
        return response.setComplete();
    }
}