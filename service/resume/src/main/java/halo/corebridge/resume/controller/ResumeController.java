package halo.corebridge.resume.controller;

import halo.corebridge.resume.model.dto.ResumeDto;
import halo.corebridge.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping
    public ResumeDto.ResumeResponse create(@RequestBody ResumeDto.ResumeCreateRequest request) {
        return resumeService.create(request);
    }

    @GetMapping("/{resumeId}")
    public ResumeDto.ResumeResponse get(@PathVariable Long resumeId) {
        return resumeService.get(resumeId);
    }

    @PostMapping("/{resumeId}/upload")
    public void upload(@PathVariable Long resumeId,
                       @RequestBody ResumeDto.ResumeUploadRequest request) {
        resumeService.upload(resumeId, request);
    }
}