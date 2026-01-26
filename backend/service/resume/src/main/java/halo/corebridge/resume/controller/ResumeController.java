package halo.corebridge.resume.controller;

import halo.corebridge.common.response.BaseResponse;
import halo.corebridge.resume.model.dto.ResumeDto;
import halo.corebridge.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    // ============================================
    // 이력서 조회/업데이트
    // ============================================

    /**
     * 내 이력서 조회 (없으면 자동 생성)
     */
    @GetMapping("/me")
    public BaseResponse<ResumeDto.ResumeResponse> getMyResume(
            @AuthenticationPrincipal Long userId) {
        return BaseResponse.success(resumeService.getOrCreate(userId));
    }

    /**
     * 내 이력서 업데이트
     */
    @PutMapping("/me")
    public BaseResponse<ResumeDto.ResumeResponse> updateMyResume(
            @AuthenticationPrincipal Long userId,
            @RequestBody ResumeDto.UpdateRequest request) {
        return BaseResponse.success(resumeService.update(userId, request));
    }

    // ============================================
    // 버전 관리
    // ============================================

    /**
     * 버전 목록 조회
     */
    @GetMapping("/me/versions")
    public BaseResponse<ResumeDto.VersionListResponse> getVersions(
            @AuthenticationPrincipal Long userId) {
        return BaseResponse.success(resumeService.getVersions(userId));
    }

    /**
     * 특정 버전 조회
     */
    @GetMapping("/me/versions/{version}")
    public BaseResponse<ResumeDto.VersionResponse> getVersion(
            @AuthenticationPrincipal Long userId,
            @PathVariable int version) {
        return BaseResponse.success(resumeService.getVersion(userId, version));
    }

    /**
     * 특정 버전으로 복원
     */
    @PostMapping("/me/versions/{version}/restore")
    public BaseResponse<ResumeDto.ResumeResponse> restoreVersion(
            @AuthenticationPrincipal Long userId,
            @PathVariable int version) {
        return BaseResponse.success(resumeService.restoreVersion(userId, version));
    }

    // ============================================
    // AI 분석
    // ============================================

    /**
     * AI 분석 요청
     */
    @PostMapping("/me/analyze")
    public BaseResponse<ResumeDto.ResumeResponse> requestAnalysis(
            @AuthenticationPrincipal Long userId) {
        return BaseResponse.success(resumeService.requestAnalysis(userId));
    }

    /**
     * AI 분석 결과 콜백 (내부 서비스용)
     */
    @PostMapping("/{resumeId}/ai-result")
    public BaseResponse<ResumeDto.ResumeResponse> updateAiResult(
            @PathVariable Long resumeId,
            @RequestBody ResumeDto.AiResultRequest request) {
        return BaseResponse.success(resumeService.updateAiResult(resumeId, request));
    }
}
