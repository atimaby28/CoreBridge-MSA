package halo.corebridge.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Gateway Rate Limiter 설정
 *
 * Apply API에 대한 요청 속도를 제한하여 서비스를 보호합니다.
 * Redis Token Bucket 알고리즘 기반으로 동작합니다.
 *
 * 설정값 (application.yml):
 * - replenishRate: 100  → 초당 100건 허용
 * - burstCapacity: 200  → 순간 최대 200건까지 허용
 *
 * 초과 시 429 Too Many Requests 응답을 반환합니다.
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Apply API Rate Limiter 키 리졸버
     *
     * - JWT 인증된 유저: userId 기준 제한 (유저별 요청 속도 제한)
     * - 미인증 요청: IP 기준 제한
     *
     * Gateway의 JwtAuthenticationFilter가 먼저 실행되어
     * X-User-Id 헤더를 주입하므로, 이 헤더로 유저를 식별합니다.
     */
    @Bean
    public KeyResolver applyRateLimiterKeyResolver() {
        return exchange -> {
            // Gateway JwtAuthenticationFilter가 주입한 X-User-Id 헤더 확인
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null) {
                return Mono.just("user:" + userId);
            }
            // 미인증 요청은 IP 기준
            String ip = exchange.getRequest()
                    .getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + ip);
        };
    }
}
