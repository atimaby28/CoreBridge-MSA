package halo.corebridge.jobposting.controller;

import halo.corebridge.common.web.ApiPaths;
import halo.corebridge.jobposting.model.dto.JobpostingDto;
import halo.corebridge.jobposting.service.JobpostingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.JOB_POSTINGS)
public class JobpostingController {
    private final JobpostingService jobpostingService;

    @GetMapping("{jobpostingId}")
    public JobpostingDto.JobpostingResponse read(@PathVariable Long jobpostingId) {
        return jobpostingService.read(jobpostingId);
    }

    @GetMapping
    public JobpostingDto.JobpostingPageResponse readAll(
            @RequestParam("boardId") Long boardId,
            @RequestParam("page") Long page,
            @RequestParam("pageSize") Long pageSize
    ) {
        return jobpostingService.readAll(boardId, page, pageSize);
    }

    @PostMapping
    public JobpostingDto.JobpostingResponse create(@RequestBody JobpostingDto.JobpostingCreateRequest request) {
        return jobpostingService.create(request);
    }

    @PutMapping("{jobpostingId}")
    public JobpostingDto.JobpostingResponse update(@PathVariable Long jobpostingId, @RequestBody JobpostingDto.JobpostingUpdateRequest request) {
        return jobpostingService.update(jobpostingId, request);
    }

    @DeleteMapping("{jobpostingId}")
    public void delete(@PathVariable Long jobpostingId) {
        jobpostingService.delete(jobpostingId);
    }
}
