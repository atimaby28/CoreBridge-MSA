package halo.corebridge.common.audit.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditLogRequest {

    private Long userId;
    private String userEmail;
    private String serviceName;
    private String eventType;
    private String httpMethod;
    private String requestUri;
    private String clientIp;
    private String userAgent;
    private Integer httpStatus;
    private Long executionTime;
    private String requestBody;
    private String errorMessage;
}
