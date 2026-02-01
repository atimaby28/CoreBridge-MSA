package halo.corebridge.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Gateway 환경에서 사용하는 간소화된 Security 설정
 * - JWT 검증은 Gateway에서 수행
 * - 서비스는 Gateway가 전달한 헤더만 사용
 */
public abstract class BaseSecurityConfig {

    /**
     * 기본 Security 설정 (하위 클래스에서 커스터마이징 가능)
     */
    protected SecurityFilterChain configureFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (내부 통신)
                .csrf(AbstractHttpConfigurer::disable)

                // 세션 비활성화 (Stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Gateway 인증 필터 추가
                .addFilterBefore(gatewayAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public GatewayAuthenticationFilter gatewayAuthenticationFilter() {
        return new GatewayAuthenticationFilter();
    }
}
