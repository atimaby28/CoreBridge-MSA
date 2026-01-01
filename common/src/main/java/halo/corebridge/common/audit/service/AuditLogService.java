package halo.corebridge.common.audit.service;


import halo.corebridge.common.audit.model.dto.AuditDto;
import halo.corebridge.common.audit.model.entity.AuditLog;
import halo.corebridge.common.audit.repository.AuditLogRepository;
import halo.corebridge.infra.id.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository repository;
    private final Snowflake snowflake = new Snowflake();

    @Async
    public void log(AuditDto.AuditRequest request) {
        try {
            AuditLog auditLog = AuditLog.create(
                    snowflake.nextId(),
                    request.getUserId(),
                    request.getServiceName(),
                    request.getMethod(),
                    request.getUri(),
                    request.getAction(),
                    request.getIp(),
                    request.getStatusCode()
            );

            repository.save(auditLog);

        } catch (Exception e) {
            log.error(
                    "[AUDIT_SAVE_FAILED] service={} userId={} method={} uri={} action={} ip={} status={}",
                    request.getServiceName(),
                    request.getUserId(),
                    request.getMethod(),
                    request.getUri(),
                    request.getAction(),
                    request.getIp(),
                    request.getStatusCode(),
                    e
            );
        }
    }
}
