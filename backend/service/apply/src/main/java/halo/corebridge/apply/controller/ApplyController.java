package halo.corebridge.apply.controller;

import halo.corebridge.apply.model.dto.ApplyDto;
import halo.corebridge.apply.model.dto.ProcessDto;
import halo.corebridge.apply.model.enums.ProcessStep;
import halo.corebridge.apply.service.ApplyService;
import halo.corebridge.apply.service.ProcessService;
import halo.corebridge.common.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 지원 API
 * 
 * 채용 공고 지원 관련 API를 제공합니다.
 * 상태 전이는 ProcessController에서 처리합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/applies")
public class ApplyController {

    private final ApplyService applyService;
    private final ProcessService processService;

    // ============================================
    // 지원자 (구직자) API
    // ============================================

    /**
     * 지원하기
     */
    @PostMapping
    public BaseResponse<ApplyDto.ApplyDetailResponse> apply(
            @RequestBody ApplyDto.CreateRequest request
    ) {
        return BaseResponse.success(applyService.apply(request));
    }

    /**
     * 지원 취소 (APPLIED 상태에서만)
     */
    @DeleteMapping("/{applyId}/users/{userId}")
    public BaseResponse<Void> cancel(
            @PathVariable("applyId") Long applyId,
            @PathVariable("userId") Long userId
    ) {
        applyService.cancel(applyId, userId);
        return BaseResponse.success();
    }

    /**
     * 내 지원 목록 조회
     */
    @GetMapping("/users/{userId}")
    public BaseResponse<ApplyDto.ApplyPageResponse> getMyApplies(
            @PathVariable("userId") Long userId
    ) {
        return BaseResponse.success(applyService.getMyApplies(userId));
    }

    /**
     * 지원 상세 조회
     */
    @GetMapping("/{applyId}")
    public BaseResponse<ApplyDto.ApplyDetailResponse> read(
            @PathVariable("applyId") Long applyId
    ) {
        return BaseResponse.success(applyService.read(applyId));
    }

    // ============================================
    // 기업 API
    // ============================================

    /**
     * 공고별 지원자 목록 조회
     */
    @GetMapping("/jobpostings/{jobpostingId}")
    public BaseResponse<ApplyDto.ApplyPageResponse> getAppliesByJobposting(
            @PathVariable("jobpostingId") Long jobpostingId
    ) {
        return BaseResponse.success(applyService.getAppliesByJobposting(jobpostingId));
    }

    /**
     * 공고별 특정 단계 지원자 목록 조회
     */
    @GetMapping("/jobpostings/{jobpostingId}/steps/{step}")
    public BaseResponse<ApplyDto.ApplyPageResponse> getAppliesByStep(
            @PathVariable("jobpostingId") Long jobpostingId,
            @PathVariable("step") ProcessStep step
    ) {
        return BaseResponse.success(applyService.getAppliesByStep(jobpostingId, step));
    }

    /**
     * 메모 수정 (기업 내부용)
     */
    @PatchMapping("/{applyId}/memo")
    public BaseResponse<ApplyDto.ApplyDetailResponse> updateMemo(
            @PathVariable("applyId") Long applyId,
            @RequestBody ApplyDto.UpdateMemoRequest request
    ) {
        return BaseResponse.success(applyService.updateMemo(applyId, request));
    }

    /**
     * 지원 이력 조회 (상태 변경 이력)
     */
    @GetMapping("/{applyId}/history")
    public BaseResponse<List<ProcessDto.HistoryResponse>> getHistory(
            @PathVariable("applyId") Long applyId
    ) {
        return BaseResponse.success(processService.getHistoryByApplyId(applyId));
    }

    // ============================================
    // 통계 API (Dashboard용)
    // ============================================

    /**
     * 사용자별 지원 통계 (구직자용)
     */
    @GetMapping("/users/{userId}/stats")
    public BaseResponse<ProcessDto.UserStatsResponse> getUserStats(
            @PathVariable("userId") Long userId
    ) {
        return BaseResponse.success(processService.getUserStats(userId));
    }

    /**
     * 공고별 지원자 통계 (기업용)
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
