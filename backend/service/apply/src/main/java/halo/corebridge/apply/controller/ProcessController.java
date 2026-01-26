package halo.corebridge.apply.controller;

import halo.corebridge.apply.model.dto.ProcessDto;
import halo.corebridge.apply.model.enums.ProcessStep;
import halo.corebridge.apply.service.ProcessService;
import halo.corebridge.common.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 채용 프로세스 API (State Machine)
 * 
 * 지원자의 채용 프로세스 상태 조회 및 전이를 담당합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/processes")
public class ProcessController {

    private final ProcessService processService;

    // ============================================
    // 상태 전이 (State Machine 핵심)
    // ============================================

    /**
     * 상태 전이 (기업 담당자용)
     */
    @PatchMapping("/{processId}/transition")
    public BaseResponse<ProcessDto.ProcessResponse> transition(
            @PathVariable("processId") Long processId,
            @RequestBody ProcessDto.TransitionRequest request
    ) {
        return BaseResponse.success(processService.transition(processId, request));
    }

    /**
     * 지원 ID로 상태 전이
     */
    @PatchMapping("/applies/{applyId}/transition")
    public BaseResponse<ProcessDto.ProcessResponse> transitionByApply(
            @PathVariable("applyId") Long applyId,
            @RequestBody ProcessDto.TransitionRequest request
    ) {
        return BaseResponse.success(processService.transitionByApplyId(applyId, request));
    }

    // ============================================
    // 조회
    // ============================================

    /**
     * 프로세스 상세 조회
     */
    @GetMapping("/{processId}")
    public BaseResponse<ProcessDto.ProcessResponse> read(@PathVariable("processId") Long processId) {
        return BaseResponse.success(processService.read(processId));
    }

    /**
     * 지원 ID로 프로세스 조회
     */
    @GetMapping("/applies/{applyId}")
    public BaseResponse<ProcessDto.ProcessResponse> readByApply(
            @PathVariable("applyId") Long applyId
    ) {
        return BaseResponse.success(processService.readByApplyId(applyId));
    }

    /**
     * 공고별 프로세스 목록 (기업용)
     */
    @GetMapping("/jobpostings/{jobpostingId}")
    public BaseResponse<ProcessDto.ProcessPageResponse> getByJobposting(
            @PathVariable("jobpostingId") Long jobpostingId
    ) {
        return BaseResponse.success(processService.getByJobposting(jobpostingId));
    }

    /**
     * 공고별 특정 단계 프로세스 목록
     */
    @GetMapping("/jobpostings/{jobpostingId}/steps/{step}")
    public BaseResponse<ProcessDto.ProcessPageResponse> getByJobpostingAndStep(
            @PathVariable("jobpostingId") Long jobpostingId,
            @PathVariable("step") ProcessStep step
    ) {
        return BaseResponse.success(processService.getByJobpostingAndStep(jobpostingId, step));
    }

    /**
     * 내 지원 현황 (구직자용)
     */
    @GetMapping("/users/{userId}")
    public BaseResponse<ProcessDto.ProcessPageResponse> getByUser(@PathVariable("userId") Long userId) {
        return BaseResponse.success(processService.getByUser(userId));
    }

    // ============================================
    // 이력 조회
    // ============================================

    /**
     * 상태 변경 이력 조회
     */
    @GetMapping("/{processId}/history")
    public BaseResponse<List<ProcessDto.HistoryResponse>> getHistory(
            @PathVariable("processId") Long processId
    ) {
        return BaseResponse.success(processService.getHistory(processId));
    }

    /**
     * 지원 ID로 상태 변경 이력 조회
     */
    @GetMapping("/applies/{applyId}/history")
    public BaseResponse<List<ProcessDto.HistoryResponse>> getHistoryByApply(
            @PathVariable("applyId") Long applyId
    ) {
        return BaseResponse.success(processService.getHistoryByApplyId(applyId));
    }

    // ============================================
    // 메타 정보
    // ============================================

    /**
     * 모든 단계 정보 조회 (프론트엔드용)
     * 
     * 각 단계별 허용된 전이 정보를 포함합니다.
     */
    @GetMapping("/steps")
    public BaseResponse<List<ProcessDto.StepInfoResponse>> getAllSteps() {
        return BaseResponse.success(processService.getAllSteps());
    }

    // ============================================
    // 통계 API (Dashboard용)
    // ============================================

    /**
     * 사용자 통계 (구직자용)
     */
    @GetMapping("/users/{userId}/stats")
    public BaseResponse<ProcessDto.UserStatsResponse> getUserStats(
            @PathVariable("userId") Long userId
    ) {
        return BaseResponse.success(processService.getUserStats(userId));
    }

    /**
     * 공고별 통계 (기업용)
     */
    @GetMapping("/jobpostings/{jobpostingId}/stats")
    public BaseResponse<ProcessDto.CompanyStatsResponse> getJobpostingStats(
            @PathVariable("jobpostingId") Long jobpostingId
    ) {
        return BaseResponse.success(processService.getJobpostingStats(jobpostingId));
    }

    /**
     * 기업 전체 통계 (여러 공고 합산)
     */
    @PostMapping("/company/stats")
    public BaseResponse<ProcessDto.CompanyStatsResponse> getCompanyStats(
            @RequestBody List<Long> jobpostingIds
    ) {
        return BaseResponse.success(processService.getCompanyStats(jobpostingIds));
    }
}
