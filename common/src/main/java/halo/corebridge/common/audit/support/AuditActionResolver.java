package halo.corebridge.common.audit.support;

import halo.corebridge.common.audit.model.enums.AuditEventType;

import jakarta.servlet.http.HttpServletRequest;

public final class AuditActionResolver {

    private AuditActionResolver() {}

    public static String resolve(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        if (uri.contains("/login")) {
            return AuditEventType.LOGIN.name();
        }

        if (uri.contains("/users") && "POST".equals(method)) {
            return AuditEventType.USER_CREATE.name();
        }

        if (uri.contains("/users") && "PUT".equals(method)) {
            return AuditEventType.USER_UPDATE.name();
        }

        if (uri.contains("/users") && "DELETE".equals(method)) {
            return AuditEventType.USER_DELETE.name();
        }

        return AuditEventType.UNKNOWN.name();
    }
}
