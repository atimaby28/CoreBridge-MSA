package halo.corebridge.jobpostingview.controller;

import halo.corebridge.common.response.BaseResponse;
import halo.corebridge.jobpostingview.service.JobpostingViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/jobposting-views")
public class JobpostingViewController {

    private final JobpostingViewService jobpostingViewService;

    /**
     * 조회수 증가 (인증 필요)
     */
    @PostMapping("/jobpostings/{jobpostingId}")
    public BaseResponse<Long> increase(
            @PathVariable("jobpostingId") Long jobpostingId,
            @AuthenticationPrincipal Long userId
    ) {
        return BaseResponse.success(jobpostingViewService.increase(jobpostingId, userId));
    }

    /**
     * 조회수 조회 (인증 불필요)
     */
    @GetMapping("/jobpostings/{jobpostingId}/count")
    public BaseResponse<Long> count(@PathVariable("jobpostingId") Long jobpostingId) {
        return BaseResponse.success(jobpostingViewService.count(jobpostingId));
    }
}
