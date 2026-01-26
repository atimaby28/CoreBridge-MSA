package halo.corebridge.apply.service;

import halo.corebridge.apply.model.dto.ApplyDto;
import halo.corebridge.apply.model.entity.Apply;
import halo.corebridge.apply.model.entity.RecruitmentProcess;
import halo.corebridge.apply.model.enums.ProcessStep;
import halo.corebridge.apply.repository.ApplyRepository;
import halo.corebridge.apply.repository.RecruitmentProcessRepository;
import halo.corebridge.common.exception.BaseException;
import halo.corebridge.common.response.BaseResponseStatus;
import halo.corebridge.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 지원 서비스
 * 
 * 채용 공고 지원 관련 비즈니스 로직을 처리합니다.
 * 지원 시 RecruitmentProcess도 함께 생성하여 하나의 트랜잭션으로 관리합니다.
 */
@Service
@RequiredArgsConstructor
public class ApplyService {

    private final Snowflake snowflake = new Snowflake();
    private final ApplyRepository applyRepository;
    private final RecruitmentProcessRepository processRepository;
    private final ProcessService processService;

    // ============================================
    // 지원자 (구직자) 기능
    // ============================================

    /**
     * 지원하기
     * 
     * Apply와 RecruitmentProcess를 함께 생성합니다.
     * 하나의 트랜잭션으로 처리되어 데이터 일관성을 보장합니다.
     */
    @Transactional
    public ApplyDto.ApplyDetailResponse apply(ApplyDto.CreateRequest request) {
        // 중복 지원 체크
        if (applyRepository.existsByJobpostingIdAndUserId(
                request.getJobpostingId(), request.getUserId())) {
            throw new BaseException(BaseResponseStatus.ALREADY_APPLIED);
        }

        // Apply 생성
        Apply apply = Apply.create(
                snowflake.nextId(),
                request.getJobpostingId(),
                request.getUserId(),
                request.getResumeId(),
                request.getCoverLetter()
        );
        applyRepository.save(apply);

        // RecruitmentProcess 생성 (State Machine 시작)
        RecruitmentProcess process = processService.createProcess(
                apply.getApplyId(),
                apply.getJobpostingId(),
                apply.getUserId()
        );

        return ApplyDto.ApplyDetailResponse.from(apply, process);
    }

    /**
     * 지원 취소 (APPLIED 상태에서만 가능)
     */
    @Transactional
    public void cancel(Long applyId, Long userId) {
        Apply apply = applyRepository.findById(applyId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.APPLICATION_NOT_FOUND));

        // 본인 확인
        if (!apply.getUserId().equals(userId)) {
            throw new BaseException(BaseResponseStatus.ACCESS_DENIED);
        }

        // 프로세스 상태 확인 (APPLIED 상태에서만 취소 가능)
        RecruitmentProcess process = processRepository.findByApplyId(applyId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.PROCESS_NOT_FOUND));

        if (process.getCurrentStep() != ProcessStep.APPLIED) {
            throw new BaseException(BaseResponseStatus.CANNOT_CANCEL_IN_PROGRESS);
        }

        // 삭제
        processRepository.delete(process);
        applyRepository.delete(apply);
    }

    /**
     * 내 지원 목록 조회
     */
    @Transactional(readOnly = true)
    public ApplyDto.ApplyPageResponse getMyApplies(Long userId) {
        List<Apply> applies = applyRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        List<ApplyDto.ApplyDetailResponse> responses = applies.stream()
                .map(apply -> {
                    RecruitmentProcess process = processRepository.findByApplyId(apply.getApplyId())
                            .orElse(null);
                    return process != null 
                            ? ApplyDto.ApplyDetailResponse.from(apply, process)
                            : null;
                })
                .filter(r -> r != null)
                .toList();

        Long count = applyRepository.countByUserId(userId);

        return ApplyDto.ApplyPageResponse.of(responses, count);
    }

    /**
     * 지원 상세 조회
     */
    @Transactional(readOnly = true)
    public ApplyDto.ApplyDetailResponse read(Long applyId) {
        Apply apply = applyRepository.findById(applyId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.APPLICATION_NOT_FOUND));

        RecruitmentProcess process = processRepository.findByApplyId(applyId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.PROCESS_NOT_FOUND));

        return ApplyDto.ApplyDetailResponse.from(apply, process);
    }

    // ============================================
    // 기업 기능
    // ============================================

    /**
     * 공고별 지원자 목록 조회
     */
    @Transactional(readOnly = true)
    public ApplyDto.ApplyPageResponse getAppliesByJobposting(Long jobpostingId) {
        List<Apply> applies = applyRepository.findByJobpostingIdOrderByCreatedAtDesc(jobpostingId);

        List<ApplyDto.ApplyDetailResponse> responses = applies.stream()
                .map(apply -> {
                    RecruitmentProcess process = processRepository.findByApplyId(apply.getApplyId())
                            .orElse(null);
                    return process != null 
                            ? ApplyDto.ApplyDetailResponse.from(apply, process)
                            : null;
                })
                .filter(r -> r != null)
                .toList();

        Long count = applyRepository.countByJobpostingId(jobpostingId);

        return ApplyDto.ApplyPageResponse.of(responses, count);
    }

    /**
     * 공고별 특정 단계 지원자 목록 조회
     */
    @Transactional(readOnly = true)
    public ApplyDto.ApplyPageResponse getAppliesByStep(Long jobpostingId, ProcessStep step) {
        List<RecruitmentProcess> processes = processRepository
                .findByJobpostingIdAndCurrentStepOrderByStepChangedAtDesc(jobpostingId, step);

        List<ApplyDto.ApplyDetailResponse> responses = processes.stream()
                .map(process -> {
                    Apply apply = applyRepository.findById(process.getApplyId())
                            .orElse(null);
                    return apply != null 
                            ? ApplyDto.ApplyDetailResponse.from(apply, process)
                            : null;
                })
                .filter(r -> r != null)
                .toList();

        return ApplyDto.ApplyPageResponse.of(responses, (long) responses.size());
    }

    /**
     * 메모 수정 (기업 내부용)
     */
    @Transactional
    public ApplyDto.ApplyDetailResponse updateMemo(Long applyId, ApplyDto.UpdateMemoRequest request) {
        Apply apply = applyRepository.findById(applyId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.APPLICATION_NOT_FOUND));

        apply.updateMemo(request.getMemo());

        RecruitmentProcess process = processRepository.findByApplyId(applyId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.PROCESS_NOT_FOUND));

        return ApplyDto.ApplyDetailResponse.from(apply, process);
    }

    // ============================================
    // 통계 (ProcessService로 위임)
    // ============================================

    /**
     * 사용자별 지원 통계
     */
    @Transactional(readOnly = true)
    public Object getUserStats(Long userId) {
        return processService.getUserStats(userId);
    }

    /**
     * 공고별 지원자 통계
     */
    @Transactional(readOnly = true)
    public Object getJobpostingStats(Long jobpostingId) {
        return processService.getJobpostingStats(jobpostingId);
    }

    /**
     * 기업 전체 통계
     */
    @Transactional(readOnly = true)
    public Object getCompanyStats(List<Long> jobpostingIds) {
        return processService.getCompanyStats(jobpostingIds);
    }
}
