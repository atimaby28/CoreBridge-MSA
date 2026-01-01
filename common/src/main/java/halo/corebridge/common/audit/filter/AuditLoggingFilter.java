package halo.corebridge.common.audit.filter;

import halo.corebridge.common.audit.model.dto.AuditDto;
import halo.corebridge.common.audit.service.AuditLogService;
import halo.corebridge.common.audit.support.AuditActionResolver;
import halo.corebridge.common.audit.support.AuditUserResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuditLoggingFilter extends OncePerRequestFilter {

    private final AuditLogService auditLogService;

    @Value("${audit.service-name}")
    private String serviceName;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. 요청 먼저 처리
        filterChain.doFilter(request, response);

        // 2. 사용자 식별
        Long userId = AuditUserResolver.resolveUserId();

        // 3. request에서 "값만" 복사
        AuditDto.AuditRequest auditRequest = AuditDto.AuditRequest.builder()
                .userId(userId)
                .serviceName(serviceName)
                .method(request.getMethod())
                .uri(request.getRequestURI())
                .action(AuditActionResolver.resolve(request))
                .ip(request.getRemoteAddr())
                .statusCode(response.getStatus())
                .build();

        // 4. DTO만 넘김 (안전)
        auditLogService.log(auditRequest);
    }
}
