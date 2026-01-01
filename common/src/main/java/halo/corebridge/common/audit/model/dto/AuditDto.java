package halo.corebridge.common.audit.model.dto;

import lombok.Builder;
import lombok.Getter;

public class AuditDto {

    @Getter
    @Builder
    public static class AuditRequest {
        private Long userId;
        private String serviceName;
        private String method;
        private String uri;
        private String ip;
        private String action;
        private int statusCode;
    }
}
