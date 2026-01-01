package halo.corebridge.common.audit.support;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuditUserResolver {

    private AuditUserResolver() {}

    public static Long resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();

        // CustomUserDetails에 userId 노출되어 있다는 전제
        try {
            return (Long) principal.getClass()
                    .getMethod("getUserId")
                    .invoke(principal);
        } catch (Exception e) {
            return null;
        }
    }
}
