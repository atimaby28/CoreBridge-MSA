package halo.corebridge.schedule.config;

import halo.corebridge.schedule.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 공개 엔드포인트
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // 일정 조회는 인증된 사용자만
                        .requestMatchers(HttpMethod.GET, "/api/v1/schedules/**").authenticated()
                        // 일정 생성/수정/삭제는 기업만
                        .requestMatchers(HttpMethod.POST, "/api/v1/schedules/**").hasRole("COMPANY")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/schedules/**").hasRole("COMPANY")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/schedules/**").hasRole("COMPANY")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/schedules/**").hasRole("COMPANY")
                        // 내부 API
                        .requestMatchers("/internal/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
