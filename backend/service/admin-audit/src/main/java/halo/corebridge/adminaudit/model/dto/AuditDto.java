package halo.corebridge.adminaudit.model.dto;

import halo.corebridge.adminaudit.model.entity.AuditLog;
import halo.corebridge.adminaudit.model.enums.AuditEventType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class AuditDto {

    // ============================================
    // Request
    // ============================================

    @Getter
    @Builder
    public static class CreateRequest {
        private Long userId;
        private String userEmail;
        private String serviceName;
        private AuditEventType eventType;
        private String httpMethod;
        private String requestUri;
        private String clientIp;
        private String userAgent;
        private Integer httpStatus;
        private Long executionTime;
        private String requestBody;
        private String errorMessage;
    }

    @Getter
    @Builder
    public static class SearchRequest {
        private Long userId;
        private String serviceName;
        private AuditEventType eventType;
        private String httpMethod;
        private Integer httpStatus;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
    }

    // ============================================
    // Response
    // ============================================

    @Getter
    @Builder
    public static class AuditResponse {
        private Long auditId;
        private Long userId;
        private String userEmail;
        private String serviceName;
        private AuditEventType eventType;
        private String eventTypeName;
        private String httpMethod;
        private String requestUri;
        private String clientIp;
        private String userAgent;
        private Integer httpStatus;
        private Long executionTime;
        private String requestBody;
        private String errorMessage;
        private LocalDateTime createdAt;

        public static AuditResponse from(AuditLog log) {
            return AuditResponse.builder()
                    .auditId(log.getAuditId())
                    .userId(log.getUserId())
                    .userEmail(log.getUserEmail())
                    .serviceName(log.getServiceName())
                    .eventType(log.getEventType())
                    .eventTypeName(log.getEventType() != null ? log.getEventType().getDisplayName() : null)
                    .httpMethod(log.getHttpMethod())
                    .requestUri(log.getRequestUri())
                    .clientIp(log.getClientIp())
                    .userAgent(log.getUserAgent())
                    .httpStatus(log.getHttpStatus())
                    .executionTime(log.getExecutionTime())
                    .requestBody(log.getRequestBody())
                    .errorMessage(log.getErrorMessage())
                    .createdAt(log.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class AuditPageResponse {
        private List<AuditResponse> audits;
        private Long totalCount;
        private Integer page;
        private Integer size;
        private Integer totalPages;
        private Boolean hasNext;
        private Boolean hasPrevious;

        public static AuditPageResponse of(List<AuditResponse> audits, Long totalCount) {
            return AuditPageResponse.builder()
                    .audits(audits)
                    .totalCount(totalCount)
                    .build();
        }

        public static AuditPageResponse of(List<AuditResponse> audits, Long totalCount, Integer page, Integer size) {
            int totalPages = (int) Math.ceil((double) totalCount / size);
            return AuditPageResponse.builder()
                    .audits(audits)
                    .totalCount(totalCount)
                    .page(page)
                    .size(size)
                    .totalPages(totalPages)
                    .hasNext(page < totalPages - 1)
                    .hasPrevious(page > 0)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class AuditStatsResponse {
        private Long totalRequests;
        private Long errorCount;
        private Long uniqueUsers;
        private Double avgExecutionTime;
        private String mostActiveService;
        private AuditEventType mostFrequentEvent;
    }
}
