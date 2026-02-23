package halo.corebridge.schedule.repository;

import halo.corebridge.schedule.model.entity.Schedule;
import halo.corebridge.schedule.model.enums.ScheduleStatus;
import halo.corebridge.schedule.model.enums.ScheduleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // ============================================
    // 지원자용 조회
    // ============================================

    /**
     * 지원자의 모든 일정 조회
     */
    List<Schedule> findByUserIdOrderByStartTimeAsc(Long userId);

    /**
     * 지원자의 예정된 일정 조회
     */
    List<Schedule> findByUserIdAndStatusOrderByStartTimeAsc(Long userId, ScheduleStatus status);

    /**
     * 지원자의 특정 기간 일정 조회 (캘린더용)
     */
    @Query("SELECT s FROM Schedule s WHERE s.userId = :userId " +
            "AND s.startTime >= :start AND s.endTime <= :end " +
            "ORDER BY s.startTime ASC")
    List<Schedule> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // ============================================
    // 기업용 조회
    // ============================================

    /**
     * 기업의 모든 일정 조회
     */
    List<Schedule> findByCompanyIdOrderByStartTimeAsc(Long companyId);

    /**
     * 기업의 특정 기간 일정 조회 (캘린더용)
     */
    @Query("SELECT s FROM Schedule s WHERE s.companyId = :companyId " +
            "AND s.startTime >= :start AND s.endTime <= :end " +
            "ORDER BY s.startTime ASC")
    List<Schedule> findByCompanyIdAndDateRange(
            @Param("companyId") Long companyId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * 공고별 일정 조회
     */
    List<Schedule> findByJobpostingIdOrderByStartTimeAsc(Long jobpostingId);

    /**
     * 지원서별 일정 조회
     */
    List<Schedule> findByApplyIdOrderByStartTimeAsc(Long applyId);

    /**
     * 면접관별 일정 조회
     */
    List<Schedule> findByInterviewerIdOrderByStartTimeAsc(Long interviewerId);

    /**
     * 면접관의 특정 기간 일정 조회
     */
    @Query("SELECT s FROM Schedule s WHERE s.interviewerId = :interviewerId " +
            "AND s.startTime >= :start AND s.endTime <= :end " +
            "ORDER BY s.startTime ASC")
    List<Schedule> findByInterviewerIdAndDateRange(
            @Param("interviewerId") Long interviewerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // ============================================
    // 충돌 체크 쿼리
    // ============================================

    /**
     * 면접관 시간 충돌 체크
     * 조건: 같은 면접관, 시간대 겹침, SCHEDULED/IN_PROGRESS 상태
     */
    @Query("SELECT s FROM Schedule s WHERE s.interviewerId = :interviewerId " +
            "AND s.status IN ('SCHEDULED', 'IN_PROGRESS') " +
            "AND NOT (s.endTime <= :newStart OR s.startTime >= :newEnd) " +
            "AND (:excludeId IS NULL OR s.id != :excludeId)")
    List<Schedule> findInterviewerConflicts(
            @Param("interviewerId") Long interviewerId,
            @Param("newStart") LocalDateTime newStart,
            @Param("newEnd") LocalDateTime newEnd,
            @Param("excludeId") Long excludeId
    );

    /**
     * 지원자 시간 충돌 체크
     * 조건: 같은 지원자, 시간대 겹침, SCHEDULED/IN_PROGRESS 상태
     */
    @Query("SELECT s FROM Schedule s WHERE s.userId = :userId " +
            "AND s.status IN ('SCHEDULED', 'IN_PROGRESS') " +
            "AND NOT (s.endTime <= :newStart OR s.startTime >= :newEnd) " +
            "AND (:excludeId IS NULL OR s.id != :excludeId)")
    List<Schedule> findApplicantConflicts(
            @Param("userId") Long userId,
            @Param("newStart") LocalDateTime newStart,
            @Param("newEnd") LocalDateTime newEnd,
            @Param("excludeId") Long excludeId
    );

    // ============================================
    // 통계 쿼리
    // ============================================

    /**
     * 지원자의 예정된 일정 수
     */
    long countByUserIdAndStatus(Long userId, ScheduleStatus status);

    /**
     * 기업의 예정된 일정 수
     */
    long countByCompanyIdAndStatus(Long companyId, ScheduleStatus status);

    /**
     * 공고별 일정 수
     */
    long countByJobpostingId(Long jobpostingId);

    /**
     * 타입별 일정 수 (기업)
     */
    long countByCompanyIdAndType(Long companyId, ScheduleType type);
}
