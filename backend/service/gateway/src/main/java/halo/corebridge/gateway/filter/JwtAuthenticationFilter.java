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
            "/api/v1/jobpostings"
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

        // 1. Public 경로 체크
        if (isPublicPath(path, method)) {
            return chain.filter(exchange);
        }

        // 2. 토큰 추출
        String token = resolveToken(request);
        
        if (token == null) {
            return onError(exchange, "토큰이 없습니다", HttpStatus.UNAUTHORIZED);
        }

        // 3. 토큰 검증
        try {
            Claims claims = validateAndGetClaims(token);
            
            // 4. 검증 성공 → 헤더에 사용자 정보 추가
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
     * Public 경로인지 확인
     */
    private boolean isPublicPath(String path, String method) {
        // 완전 공개 경로
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        
        // GET 요청만 공개하는 경로
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
