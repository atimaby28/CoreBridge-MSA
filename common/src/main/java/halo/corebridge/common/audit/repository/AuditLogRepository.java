package halo.corebridge.common.audit.repository;

import halo.corebridge.common.audit.model.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
