package halo.corebridge.jobpostinghot.controller;

import halo.corebridge.common.response.BaseResponse;
import halo.corebridge.jobpostinghot.model.dto.JobpostingHotDto;
import halo.corebridge.jobpostinghot.service.JobpostingHotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/jobposting-hot")
public class JobpostingHotController {
    
    private final JobpostingHotService jobpostingHotService;

    /**
     * 오늘의 인기 공고 TOP 10 (실시간 통계 반영)
     */
    @GetMapping("/today")
    public BaseResponse<List<JobpostingHotDto.Response>> readToday() {
        return BaseResponse.success(jobpostingHotService.readTopNWithLiveStats(10));
    }

    /**
     * 특정 날짜의 인기 공고
     */
    @GetMapping("/date/{dateStr}")
    public BaseResponse<List<JobpostingHotDto.Response>> readByDate(@PathVariable("dateStr") String dateStr) {
        return BaseResponse.success(jobpostingHotService.readAll(dateStr));
    }

    /**
     * 단일 채용공고 인기 등록/갱신
     */
    @PostMapping("/register/{jobpostingId}")
    public BaseResponse<JobpostingHotDto.Response> register(@PathVariable("jobpostingId") Long jobpostingId) {
        return BaseResponse.success(jobpostingHotService.register(jobpostingId));
    }

    /**
     * 게시판별 인기 공고 갱신
     */
    @PostMapping("/boards/{boardId}/update")
    public BaseResponse<Integer> updateByBoard(@PathVariable("boardId") Long boardId) {
        return BaseResponse.success(jobpostingHotService.updateByBoard(boardId));
    }

    /**
     * 전체 인기 공고 갱신
     */
    @PostMapping("/update-all")
    public BaseResponse<Integer> updateAll() {
        return BaseResponse.success(jobpostingHotService.updateAll());
    }
}
