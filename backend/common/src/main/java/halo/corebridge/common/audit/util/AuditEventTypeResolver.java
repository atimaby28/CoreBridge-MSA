package halo.corebridge.common.audit.util;

import halo.corebridge.common.audit.enums.AuditEventType;
import org.springframework.stereotype.Component;

/**
 * HTTP 요청 정보를 기반으로 감사 이벤트 타입을 결정
 */
@Component
public class AuditEventTypeResolver {

    public AuditEventType resolve(String httpMethod, String requestUri, int httpStatus) {
        // 에러 응답인 경우
        if (httpStatus >= 500) {
            return AuditEventType.SYSTEM_ERROR;
        }

        String method = httpMethod.toUpperCase();
        String uri = requestUri.toLowerCase();

        // 인증 관련
        if (uri.contains("/login")) {
            return httpStatus >= 400 ? AuditEventType.LOGIN_FAILED : AuditEventType.LOGIN;
        }
        if (uri.contains("/logout")) {
            return AuditEventType.LOGOUT;
        }
        if (uri.contains("/signup")) {
            return AuditEventType.USER_CREATE;
        }

        // 사용자 관련
        if (uri.matches(".*/users(/\\d+)?$") || uri.matches(".*/users/email/.*")) {
            return switch (method) {
                case "POST" -> AuditEventType.USER_CREATE;
                case "PUT", "PATCH" -> AuditEventType.USER_UPDATE;
                case "DELETE" -> AuditEventType.USER_DELETE;
                default -> AuditEventType.API_REQUEST;
            };
        }

        // 채용공고 관련
        if (uri.contains("/jobposting") || uri.contains("/job-posting")) {
            return switch (method) {
                case "POST" -> AuditEventType.JOBPOSTING_CREATE;
                case "PUT", "PATCH" -> AuditEventType.JOBPOSTING_UPDATE;
                case "DELETE" -> AuditEventType.JOBPOSTING_DELETE;
                case "GET" -> AuditEventType.JOBPOSTING_READ;
                default -> AuditEventType.API_REQUEST;
            };
        }

        // 지원 관련
        if (uri.contains("/application")) {
            return switch (method) {
                case "POST" -> AuditEventType.APPLICATION_CREATE;
                case "PUT", "PATCH" -> uri.contains("/cancel") 
                        ? AuditEventType.APPLICATION_CANCEL 
                        : AuditEventType.APPLICATION_STATUS_CHANGE;
                case "DELETE" -> AuditEventType.APPLICATION_CANCEL;
                default -> AuditEventType.API_REQUEST;
            };
        }

        // 이력서 관련
        if (uri.contains("/resume")) {
            return switch (method) {
                case "POST" -> AuditEventType.RESUME_CREATE;
                case "PUT", "PATCH" -> AuditEventType.RESUME_UPDATE;
                case "DELETE" -> AuditEventType.RESUME_DELETE;
                default -> AuditEventType.API_REQUEST;
            };
        }

        // 일정(면접) 관련
        if (uri.contains("/schedule") || uri.contains("/interview")) {
            return switch (method) {
                case "POST" -> AuditEventType.SCHEDULE_CREATE;
                case "PUT", "PATCH" -> AuditEventType.SCHEDULE_UPDATE;
                case "DELETE" -> AuditEventType.SCHEDULE_CANCEL;
                default -> AuditEventType.API_REQUEST;
            };
        }

        // 알림 관련
        if (uri.contains("/notification")) {
            if (uri.contains("/read") || "PUT".equals(method) || "PATCH".equals(method)) {
                return AuditEventType.NOTIFICATION_READ;
            }
        }

        return AuditEventType.API_REQUEST;
    }
}
