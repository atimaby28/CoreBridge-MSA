package halo.corebridge.jobposting.controller;

import halo.corebridge.common.response.BaseResponse;
import halo.corebridge.jobposting.model.dto.JobpostingDto;
import halo.corebridge.jobposting.service.JobpostingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/jobpostings")
public class JobpostingController {

    private final JobpostingService jobpostingService;

    // ============================================
    // 조회 API (인증 불필요)
    // ============================================

    /**
     * 채용공고 단건 조회
     */
    @GetMapping("/{jobpostingId}")
    public BaseResponse<JobpostingDto.JobpostingResponse> read(@PathVariable Long jobpostingId) {
        return BaseResponse.success(jobpostingService.read(jobpostingId));
    }

    /**
     * 채용공고 목록 조회 (페이징)
     */
    @GetMapping
    public BaseResponse<JobpostingDto.JobpostingPageResponse> readAll(
            @RequestParam("boardId") Long boardId,
            @RequestParam("page") Long page,
            @RequestParam("pageSize") Long pageSize
    ) {
        return BaseResponse.success(jobpostingService.readAll(boardId, page, pageSize));
    }

    /**
     * 작성자별 채용공고 조회
     */
    @GetMapping("/writers/{writerId}")
    public BaseResponse<JobpostingDto.JobpostingListResponse> readByWriter(@PathVariable Long writerId) {
        return BaseResponse.success(jobpostingService.readByWriter(writerId));
    }

    // ============================================
    // 생성/수정/삭제 API (인증 필요)
    // ============================================

    /**
     * 채용공고 생성
     * - SecurityContext에서 인증된 userId 사용
     */
    @PostMapping
    public BaseResponse<JobpostingDto.JobpostingResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody JobpostingDto.JobpostingCreateRequest request
    ) {
        return BaseResponse.success(jobpostingService.create(userId, request));
    }

    /**
     * 채용공고 수정
     * - 본인이 작성한 공고만 수정 가능
     */
    @PutMapping("/{jobpostingId}")
    public BaseResponse<JobpostingDto.JobpostingResponse> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long jobpostingId,
            @Valid @RequestBody JobpostingDto.JobpostingUpdateRequest request
    ) {
        return BaseResponse.success(jobpostingService.update(userId, jobpostingId, request));
    }

    /**
     * 채용공고 삭제
     * - 본인이 작성한 공고만 삭제 가능
     */
    @DeleteMapping("/{jobpostingId}")
    public BaseResponse<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long jobpostingId
    ) {
        jobpostingService.delete(userId, jobpostingId);
        return BaseResponse.success();
    }

    // ============================================
    // 내 채용공고 API (인증 필요)
    // ============================================

    /**
     * 내가 작성한 채용공고 목록
     */
    @GetMapping("/me")
    public BaseResponse<JobpostingDto.JobpostingListResponse> getMyJobpostings(
            @AuthenticationPrincipal Long userId
    ) {
        return BaseResponse.success(jobpostingService.readByWriter(userId));
    }
}
