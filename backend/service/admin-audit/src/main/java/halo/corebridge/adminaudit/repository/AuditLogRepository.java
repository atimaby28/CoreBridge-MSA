package halo.corebridge.adminaudit.repository;

import halo.corebridge.adminaudit.model.entity.AuditLog;
import halo.corebridge.adminaudit.model.enums.AuditEventType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // 최신순 조회
    List<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 사용자별 조회
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 서비스별 조회
    List<AuditLog> findByServiceNameOrderByCreatedAtDesc(String serviceName);

    // 이벤트 타입별 조회
    List<AuditLog> findByEventTypeOrderByCreatedAtDesc(AuditEventType eventType);

    // 기간별 조회
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :start AND :end ORDER BY a.createdAt DESC")
    List<AuditLog> findByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // 에러 로그만 조회
    @Query("SELECT a FROM AuditLog a WHERE a.httpStatus >= 400 ORDER BY a.createdAt DESC")
    List<AuditLog> findErrors(Pageable pageable);

    // 전체 수
    Long countByServiceName(String serviceName);

    // 에러 수
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.httpStatus >= 400")
    Long countErrors();

    // 기간 내 사용자 수
    @Query("SELECT COUNT(DISTINCT a.userId) FROM AuditLog a WHERE a.createdAt BETWEEN :start AND :end")
    Long countUniqueUsers(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // 평균 실행 시간
    @Query("SELECT AVG(a.executionTime) FROM AuditLog a WHERE a.executionTime IS NOT NULL")
    Double getAverageExecutionTime();

    // 서비스별 요청 수 통계
    @Query("SELECT a.serviceName, COUNT(a) FROM AuditLog a GROUP BY a.serviceName ORDER BY COUNT(a) DESC")
    List<Object[]> countByService();

    // 이벤트별 요청 수 통계
    @Query("SELECT a.eventType, COUNT(a) FROM AuditLog a GROUP BY a.eventType ORDER BY COUNT(a) DESC")
    List<Object[]> countByEventType();

    // ✅ 시간대별 요청 수 (오늘) - PostgreSQL 안전 버전
    @Query("""
        SELECT EXTRACT(HOUR FROM a.createdAt), COUNT(a)
        FROM AuditLog a
        WHERE a.createdAt >= :start AND a.createdAt < :end
        GROUP BY EXTRACT(HOUR FROM a.createdAt)
        ORDER BY EXTRACT(HOUR FROM a.createdAt)
    """)
    List<Object[]> countByHourToday(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
