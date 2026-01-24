package halo.corebridge.jobpostingread.controller;

import halo.corebridge.common.response.BaseResponse;
import halo.corebridge.jobpostingread.model.dto.JobpostingReadDto;
import halo.corebridge.jobpostingread.service.JobpostingReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/jobposting-read")
public class JobpostingReadController {
    
    private final JobpostingReadService jobpostingReadService;

    /**
     * 단일 채용공고 조회 (통계 포함)
     */
    @GetMapping("/{jobpostingId}")
    public BaseResponse<JobpostingReadDto.Response> read(@PathVariable("jobpostingId") Long jobpostingId) {
        return BaseResponse.success(jobpostingReadService.read(jobpostingId));
    }

    /**
     * 채용공고 목록 조회 (통계 포함)
     */
    @GetMapping
    public BaseResponse<JobpostingReadDto.PageResponse> readAll(
            @RequestParam("boardId") Long boardId,
            @RequestParam("page") Long page,
            @RequestParam("pageSize") Long pageSize
    ) {
        return BaseResponse.success(jobpostingReadService.readAll(boardId, page, pageSize));
    }
}
