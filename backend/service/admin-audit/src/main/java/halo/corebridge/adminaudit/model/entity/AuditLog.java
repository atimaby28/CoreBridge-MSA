package halo.corebridge.adminaudit.model.entity;

import halo.corebridge.adminaudit.model.enums.AuditEventType;
import halo.corebridge.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_user", columnList = "userId"),
    @Index(name = "idx_audit_service", columnList = "serviceName"),
    @Index(name = "idx_audit_event", columnList = "eventType"),
    @Index(name = "idx_audit_created", columnList = "createdAt DESC")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog extends BaseTimeEntity {

    @Id
    private Long auditId;

    private Long userId;              // null 가능 (anonymous)

    private String userEmail;         // 사용자 이메일 (조회 편의)

    private String serviceName;       // user, jobposting, application

    @Enumerated(EnumType.STRING)
    private AuditEventType eventType;

    private String httpMethod;        // GET, POST, PUT, DELETE

    private String requestUri;        // /api/v1/users/1

    private String clientIp;

    private String userAgent;         // 브라우저 정보

    private Integer httpStatus;       // 200, 400, 500

    private Long executionTime;       // 실행 시간 (ms)

    private String requestBody;       // 요청 본문 (민감정보 제외)

    private String errorMessage;      // 에러 발생 시 메시지

    public static AuditLog create(
            Long auditId,
            Long userId,
            String userEmail,
            String serviceName,
            AuditEventType eventType,
            String httpMethod,
            String requestUri,
            String clientIp,
            String userAgent,
            Integer httpStatus,
            Long executionTime,
            String requestBody,
            String errorMessage
    ) {
        AuditLog log = new AuditLog();
        log.auditId = auditId;
        log.userId = userId;
        log.userEmail = userEmail;
        log.serviceName = serviceName;
        log.eventType = eventType;
        log.httpMethod = httpMethod;
        log.requestUri = requestUri;
        log.clientIp = clientIp;
        log.userAgent = userAgent;
        log.httpStatus = httpStatus;
        log.executionTime = executionTime;
        log.requestBody = requestBody;
        log.errorMessage = errorMessage;
        log.createdAt = LocalDateTime.now();
        log.updatedAt = log.createdAt;
        return log;
    }

    /**
     * 간단한 생성 (필수 필드만)
     */
    public static AuditLog createSimple(
            Long auditId,
            Long userId,
            String serviceName,
            AuditEventType eventType,
            String httpMethod,
            String requestUri,
            String clientIp,
            Integer httpStatus
    ) {
        return create(
                auditId, userId, null, serviceName, eventType,
                httpMethod, requestUri, clientIp, null,
                httpStatus, null, null, null
        );
    }
}
