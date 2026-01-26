package halo.corebridge.apply.repository;

import halo.corebridge.apply.model.entity.ProcessHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcessHistoryRepository extends JpaRepository<ProcessHistory, Long> {

    // 프로세스별 이력
    List<ProcessHistory> findByProcessIdOrderByCreatedAtDesc(Long processId);

    // 지원별 이력
    List<ProcessHistory> findByApplyIdOrderByCreatedAtDesc(Long applyId);

    // 프로세스별 이력 수
    Long countByProcessId(Long processId);
}
