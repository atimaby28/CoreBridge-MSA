package halo.corebridge.resume.service;

import halo.corebridge.resume.model.dto.ResumeDto;
import halo.corebridge.resume.model.entity.Resume;
import halo.corebridge.resume.model.entity.ResumeVersion;
import halo.corebridge.resume.repository.ResumeRepository;
import halo.corebridge.resume.repository.ResumeVersionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository versionRepository;

    public ResumeDto.ResumeResponse create(ResumeDto.ResumeCreateRequest request) {
        Resume resume = new Resume(
                request.getUserId(),
                request.getTitle()
        );

        return ResumeDto.ResumeResponse.from(resumeRepository.save(resume));
    }

    public ResumeDto.ResumeResponse get(Long resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found"));

        return ResumeDto.ResumeResponse.from(resume);
    }

    public void upload(Long resumeId, ResumeDto.ResumeUploadRequest request) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found"));

        resume.uploadNewVersion();

        ResumeVersion version = new ResumeVersion(
                resume.getId(),
                resume.getCurrentVersion(),
                request.getFileUrl(),
                request.getFileType()
        );

        versionRepository.save(version);
        resumeRepository.save(resume);

        // TODO: Redis / n8n 이벤트 발행
    }
}