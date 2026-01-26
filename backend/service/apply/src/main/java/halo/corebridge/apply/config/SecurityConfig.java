package halo.corebridge.apply.config;

import halo.corebridge.apply.security.JwtAuthenticationFilter;
import halo.corebridge.common.audit.filter.AuditLoggingFilter;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuditLoggingFilter auditLoggingFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT 사용)
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 비활성화 (Stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // URL별 인가 설정
                .authorizeHttpRequests(auth -> auth
                        // 프로세스 단계 정보는 공개 (프론트엔드 UI용)
                        .requestMatchers(HttpMethod.GET, "/api/v1/processes/steps").permitAll()

                        // 지원 API - 모두 인증 필요
                        .requestMatchers("/api/v1/applies/**").authenticated()

                        // 프로세스 API - 모두 인증 필요
                        .requestMatchers("/api/v1/processes/**").authenticated()

                        // Actuator, Health check 허용
                        .requestMatchers("/actuator/**", "/health/**").permitAll()

                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )

                // ============================================
                // Filter 순서 (중요!)
                // ============================================
                // 1. JwtAuthenticationFilter - 토큰 검증 → SecurityContext 설정
                // 2. AuditLoggingFilter - SecurityContext에서 userId 추출
                // ============================================
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(auditLoggingFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    /**
     * AuditLoggingFilter가 Servlet Filter로 자동 등록되는 것을 방지
     * (Security Filter Chain에서만 동작하도록)
     */
    @Bean
    public FilterRegistrationBean<AuditLoggingFilter> auditLoggingFilterRegistration(
            AuditLoggingFilter filter) {
        FilterRegistrationBean<AuditLoggingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);  // 자동 등록 비활성화
        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
