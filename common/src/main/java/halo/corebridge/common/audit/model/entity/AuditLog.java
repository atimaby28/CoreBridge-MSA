package halo.corebridge.common.audit.model.entity;

import halo.corebridge.common.domain.base.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog extends BaseTimeEntity {

    @Id
    private Long auditId;

    private Long userId;          // null 가능 (anonymous)
    private String serviceName;   // user, jobposting
    private String httpMethod;    // GET, POST
    private String requestUri;    // /api/v1/users/1
    private String action;        // USER_UPDATE, LOGIN
    String ip;
    private int httpStatus;

    public static AuditLog create(
            Long auditId,
            Long userId,
            String serviceName,
            String httpMethod,
            String requestUri,
            String action,
            String ip,

            int httpStatus
    ) {
        AuditLog log = new AuditLog();
        log.auditId = auditId;
        log.userId = userId;
        log.serviceName = serviceName;
        log.httpMethod = httpMethod;
        log.requestUri = requestUri;
        log.action = action;
        log.ip = ip;
        log.httpStatus = httpStatus;

        return log;
    }
}
