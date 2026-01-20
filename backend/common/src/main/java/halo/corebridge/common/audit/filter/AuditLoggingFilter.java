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

    @Value("${audit.service-name:unknown}")
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
        Long userId = extractUserId(request);
        String userEmail = extractUserEmail(request);
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

    private Long extractUserId(HttpServletRequest request) {
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

    private String extractUserEmail(HttpServletRequest request) {
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
