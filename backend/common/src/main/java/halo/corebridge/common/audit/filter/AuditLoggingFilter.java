package halo.corebridge.common.audit.filter;

import halo.corebridge.common.audit.client.AuditClient;
import halo.corebridge.common.audit.dto.AuditLogRequest;
import halo.corebridge.common.audit.enums.AuditEventType;
import halo.corebridge.common.audit.util.AuditEventTypeResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * HTTP 요청/응답을 가로채서 감사 로그를 생성하는 필터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLoggingFilter extends OncePerRequestFilter {

    private final AuditClient auditClient;
    private final AuditEventTypeResolver eventTypeResolver;

    @Value("${spring.application.name:unknown}")
    private String serviceName;

    @Value("${audit.enabled:true}")
    private boolean auditEnabled;

    // 감사 로그에서 제외할 경로
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/actuator",
            "/health",
            "/swagger",
            "/v3/api-docs",
            "/favicon.ico"
    );

    // 요청 바디를 기록할 최대 크기
    private static final int MAX_REQUEST_BODY_SIZE = 1000;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 감사 로그 비활성화 또는 제외 경로인 경우 스킵
        if (!auditEnabled || shouldExclude(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 요청/응답 래핑 (바디 읽기 위함)
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();
        String errorMessage = null;

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } catch (Exception e) {
            errorMessage = e.getMessage();
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;

            // 감사 로그 생성 및 전송
            try {
                AuditLogRequest auditLogRequest = buildAuditLogRequest(
                        wrappedRequest,
                        wrappedResponse,
                        executionTime,
                        errorMessage
                );
                auditClient.sendAuditLogAsync(auditLogRequest);
            } catch (Exception e) {
                log.warn("Failed to create audit log: {}", e.getMessage());
            }

            // 응답 바디 복사 (필수!)
            wrappedResponse.copyBodyToResponse();
        }
    }

    private boolean shouldExclude(String requestUri) {
        return EXCLUDED_PATHS.stream().anyMatch(requestUri::startsWith);
    }

    private AuditLogRequest buildAuditLogRequest(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            long executionTime,
            String errorMessage
    ) {
        // SecurityContext에서 사용자 정보 추출
        Long userId = extractUserIdFromSecurityContext();
        String userEmail = extractUserEmailFromSecurityContext();

        // SecurityContext에 없으면 헤더에서 추출 (API Gateway 등에서 전달된 경우)
        if (userId == null) {
            userId = extractUserIdFromHeader(request);
        }
        if (userEmail == null) {
            userEmail = extractUserEmailFromHeader(request);
        }

        String requestBody = extractRequestBody(request);

        AuditEventType eventType = eventTypeResolver.resolve(
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus()
        );

        return AuditLogRequest.builder()
                .userId(userId)
                .userEmail(userEmail)
                .serviceName(serviceName)
                .eventType(eventType.name())
                .httpMethod(request.getMethod())
                .requestUri(request.getRequestURI())
                .clientIp(extractClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .httpStatus(response.getStatus())
                .executionTime(executionTime)
                .requestBody(requestBody)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * SecurityContext에서 사용자 ID 추출
     */
    private Long extractUserIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();

            // Principal이 Long 타입인 경우 (JWT에서 userId를 principal로 설정한 경우)
            if (principal instanceof Long) {
                return (Long) principal;
            }

            // Principal이 String 타입인 경우 (userId를 문자열로 저장한 경우)
            if (principal instanceof String) {
                try {
                    return Long.parseLong((String) principal);
                } catch (NumberFormatException e) {
                    // email이나 username인 경우 무시
                }
            }

            // Credentials에 userId가 있는 경우
            Object credentials = authentication.getCredentials();
            if (credentials instanceof Long) {
                return (Long) credentials;
            }
        }
        return null;
    }

    /**
     * SecurityContext에서 사용자 이메일 추출
     */
    private String extractUserEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {

            // Credentials에서 email 추출 (JwtAuthenticationFilter에서 설정)
            Object credentials = authentication.getCredentials();
            if (credentials instanceof String) {
                String credStr = (String) credentials;
                if (credStr.contains("@")) {
                    return credStr;
                }
            }

            // Principal이 email인 경우
            Object principal = authentication.getPrincipal();
            if (principal instanceof String) {
                String principalStr = (String) principal;
                // 이메일 형식인지 간단히 체크
                if (principalStr.contains("@")) {
                    return principalStr;
                }
            }

            // Name에서 추출 시도
            String name = authentication.getName();
            if (name != null && name.contains("@")) {
                return name;
            }
        }
        return null;
    }

    private Long extractUserIdFromHeader(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            try {
                return Long.parseLong(userIdHeader);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String extractUserEmailFromHeader(HttpServletRequest request) {
        return request.getHeader("X-User-Email");
    }

    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String extractRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }

        String body = new String(content, StandardCharsets.UTF_8);
        body = maskSensitiveData(body);

        if (body.length() > MAX_REQUEST_BODY_SIZE) {
            body = body.substring(0, MAX_REQUEST_BODY_SIZE) + "...(truncated)";
        }

        return body;
    }

    private String maskSensitiveData(String body) {
        // 비밀번호 마스킹
        body = body.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"****\"");
        body = body.replaceAll("\"currentPassword\"\\s*:\\s*\"[^\"]*\"", "\"currentPassword\":\"****\"");
        body = body.replaceAll("\"newPassword\"\\s*:\\s*\"[^\"]*\"", "\"newPassword\":\"****\"");

        // 토큰 마스킹
        body = body.replaceAll("\"token\"\\s*:\\s*\"[^\"]*\"", "\"token\":\"****\"");
        body = body.replaceAll("\"accessToken\"\\s*:\\s*\"[^\"]*\"", "\"accessToken\":\"****\"");
        body = body.replaceAll("\"refreshToken\"\\s*:\\s*\"[^\"]*\"", "\"refreshToken\":\"****\"");

        return body;
    }
}
