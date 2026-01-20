package halo.corebridge.common.audit.client;

import halo.corebridge.common.audit.dto.AuditLogRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * admin-audit 서비스로 감사 로그를 전송하는 클라이언트
 */
@Slf4j
@Component
public class AuditClient {

    private final RestTemplate restTemplate;
    private final String auditServiceUrl;
    private final boolean enabled;

    public AuditClient(
            @Qualifier("auditRestTemplate") RestTemplate restTemplate,
            @Value("${audit.service-url:http://localhost:8013}") String auditServiceUrl,
            @Value("${audit.enabled:true}") boolean enabled
    ) {
        this.restTemplate = restTemplate;
        this.auditServiceUrl = auditServiceUrl;
        this.enabled = enabled;
    }

    /**
     * 비동기로 감사 로그 전송
     * 실패해도 메인 비즈니스 로직에 영향을 주지 않음
     */
    @Async("auditExecutor")
    public void sendAuditLogAsync(AuditLogRequest request) {
        if (!enabled) {
            log.debug("Audit logging is disabled");
            return;
        }

        try {
            sendAuditLog(request);
            log.debug("Audit log sent successfully: {} {} {}",
                    request.getServiceName(),
                    request.getHttpMethod(),
                    request.getRequestUri());
        } catch (Exception e) {
            log.warn("Failed to send audit log to admin-audit service: {}", e.getMessage());
        }
    }

    /**
     * 동기로 감사 로그 전송 (중요한 이벤트용)
     */
    public void sendAuditLogSync(AuditLogRequest request) {
        if (!enabled) {
            log.debug("Audit logging is disabled");
            return;
        }

        try {
            sendAuditLog(request);
            log.debug("Audit log sent successfully (sync): {} {} {}",
                    request.getServiceName(),
                    request.getHttpMethod(),
                    request.getRequestUri());
        } catch (Exception e) {
            log.warn("Failed to send audit log to admin-audit service: {}", e.getMessage());
        }
    }

    private void sendAuditLog(AuditLogRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AuditLogRequest> entity = new HttpEntity<>(request, headers);

        String url = auditServiceUrl + "/api/v1/admin/audits";
        restTemplate.postForEntity(url, entity, String.class);
    }
}
