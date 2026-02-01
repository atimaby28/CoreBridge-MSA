package halo.corebridge.jobposting.config;

import halo.corebridge.common.audit.filter.AuditLoggingFilter;
import halo.corebridge.common.security.GatewayAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Gateway 기반 간소화된 Security 설정
 * - JWT 검증은 Gateway에서 수행
 * - 서비스는 Gateway가 전달한 X-User-* 헤더 사용
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuditLoggingFilter auditLoggingFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (내부 통신)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // 세션 비활성화 (Stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // URL별 인가 설정
                .authorizeHttpRequests(auth -> auth
                        // 조회 API는 인증 없이 허용
                        .requestMatchers(HttpMethod.GET, "/api/v1/jobpostings/**").permitAll()

                        // Actuator, Health check 허용
                        .requestMatchers("/actuator/**", "/health/**").permitAll()

                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )

                // Gateway 인증 필터 (JWT 검증 대신 헤더에서 사용자 정보 추출)
                .addFilterBefore(gatewayAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(auditLoggingFilter, GatewayAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public GatewayAuthenticationFilter gatewayAuthenticationFilter() {
        return new GatewayAuthenticationFilter();
    }

    /**
     * AuditLoggingFilter가 Servlet Filter로 자동 등록되는 것을 방지
     */
    @Bean
    public FilterRegistrationBean<AuditLoggingFilter> auditLoggingFilterRegistration(
            AuditLoggingFilter filter) {
        FilterRegistrationBean<AuditLoggingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
