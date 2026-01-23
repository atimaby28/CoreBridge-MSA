package halo.corebridge.jobpostinglike.controller;

import halo.corebridge.common.response.BaseResponse;
import halo.corebridge.jobpostinglike.dto.JobpostingLikeResponse;
import halo.corebridge.jobpostinglike.service.JobpostingLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/jobposting-likes")
public class JobpostingLikeController {

    private final JobpostingLikeService jobpostingLikeService;

    /**
     * 좋아요 상태 조회 (인증 필요)
     */
    @GetMapping("/jobpostings/{jobpostingId}")
    public BaseResponse<JobpostingLikeResponse> read(
            @PathVariable("jobpostingId") Long jobpostingId,
            @AuthenticationPrincipal Long userId
    ) {
        return BaseResponse.success(jobpostingLikeService.read(jobpostingId, userId));
    }

    /**
     * 좋아요 수 조회 (인증 불필요)
     */
    @GetMapping("/jobpostings/{jobpostingId}/count")
    public BaseResponse<Long> count(@PathVariable("jobpostingId") Long jobpostingId) {
        return BaseResponse.success(jobpostingLikeService.count(jobpostingId));
    }

    /**
     * 좋아요 (인증 필요)
     */
    @PostMapping("/jobpostings/{jobpostingId}")
    public BaseResponse<Void> like(
            @PathVariable("jobpostingId") Long jobpostingId,
            @AuthenticationPrincipal Long userId
    ) {
        jobpostingLikeService.like(jobpostingId, userId);
        return BaseResponse.success();
    }

    /**
     * 좋아요 취소 (인증 필요)
     */
    @DeleteMapping("/jobpostings/{jobpostingId}")
    public BaseResponse<Void> unlike(
            @PathVariable("jobpostingId") Long jobpostingId,
            @AuthenticationPrincipal Long userId
    ) {
        jobpostingLikeService.unlike(jobpostingId, userId);
        return BaseResponse.success();
    }
}
