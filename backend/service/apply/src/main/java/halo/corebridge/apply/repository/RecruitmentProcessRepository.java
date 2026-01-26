package halo.corebridge.apply.repository;

import halo.corebridge.apply.model.entity.RecruitmentProcess;
import halo.corebridge.apply.model.enums.ProcessStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecruitmentProcessRepository extends JpaRepository<RecruitmentProcess, Long> {

    // 지원 ID로 조회
    Optional<RecruitmentProcess> findByApplyId(Long applyId);

    // 공고별 프로세스 목록
    List<RecruitmentProcess> findByJobpostingIdOrderByCreatedAtDesc(Long jobpostingId);

    // 공고별 프로세스 수
    Long countByJobpostingId(Long jobpostingId);

    // 공고별 특정 단계의 프로세스 목록
    List<RecruitmentProcess> findByJobpostingIdAndCurrentStepOrderByStepChangedAtDesc(
            Long jobpostingId, ProcessStep step);

    // 사용자별 프로세스 목록
    List<RecruitmentProcess> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 사용자별 프로세스 수
    Long countByUserId(Long userId);

    // 특정 단계에 있는 모든 프로세스
    List<RecruitmentProcess> findByCurrentStepOrderByStepChangedAtAsc(ProcessStep step);

    // ============================================
    // 통계용 쿼리 메서드
    // ============================================

    // 사용자별 특정 단계 수
    Long countByUserIdAndCurrentStep(Long userId, ProcessStep step);

    // 사용자별 여러 단계 수
    @Query("SELECT COUNT(p) FROM RecruitmentProcess p WHERE p.userId = :userId AND p.currentStep IN :steps")
    Long countByUserIdAndCurrentStepIn(@Param("userId") Long userId, @Param("steps") List<ProcessStep> steps);

    // 공고별 특정 단계 수
    Long countByJobpostingIdAndCurrentStep(Long jobpostingId, ProcessStep step);

    // 공고별 여러 단계 수
    @Query("SELECT COUNT(p) FROM RecruitmentProcess p WHERE p.jobpostingId = :jobpostingId AND p.currentStep IN :steps")
    Long countByJobpostingIdAndCurrentStepIn(@Param("jobpostingId") Long jobpostingId, @Param("steps") List<ProcessStep> steps);

    // 여러 공고의 총 프로세스 수
    @Query("SELECT COUNT(p) FROM RecruitmentProcess p WHERE p.jobpostingId IN :jobpostingIds")
    Long countByJobpostingIdIn(@Param("jobpostingIds") List<Long> jobpostingIds);

    // 여러 공고의 단계별 수
    @Query("SELECT COUNT(p) FROM RecruitmentProcess p WHERE p.jobpostingId IN :jobpostingIds AND p.currentStep IN :steps")
    Long countByJobpostingIdInAndCurrentStepIn(@Param("jobpostingIds") List<Long> jobpostingIds, @Param("steps") List<ProcessStep> steps);
}
