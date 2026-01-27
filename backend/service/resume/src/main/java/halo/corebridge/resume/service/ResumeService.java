package halo.corebridge.resume.service;

import halo.corebridge.resume.client.AiServiceClient;
import halo.corebridge.resume.model.dto.ResumeDto;
import halo.corebridge.resume.model.entity.Resume;
import halo.corebridge.resume.model.entity.ResumeVersion;
import halo.corebridge.resume.repository.ResumeRepository;
import halo.corebridge.resume.repository.ResumeVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository versionRepository;
    private final AiServiceClient aiServiceClient;

    // ============================================
    // 이력서 조회/생성
    // ============================================

    /**
     * 사용자의 이력서 조회 (없으면 자동 생성)
     */
    @Transactional
    public ResumeDto.ResumeResponse getOrCreate(Long userId) {
        Resume resume = resumeRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("사용자 {}의 이력서 생성", userId);
                    Resume newResume = Resume.create(userId);
                    return resumeRepository.save(newResume);
                });

        return ResumeDto.ResumeResponse.from(resume);
    }

    /**
     * 이력서 ID로 조회
     */
    @Transactional(readOnly = true)
    public ResumeDto.ResumeResponse get(Long resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("이력서를 찾을 수 없습니다."));
        return ResumeDto.ResumeResponse.from(resume);
    }

    // ============================================
    // 이력서 업데이트
    // ============================================

    /**
     * 이력서 업데이트 (버전 스냅샷 자동 저장)
     */
    @Transactional
    public ResumeDto.ResumeResponse update(Long userId, ResumeDto.UpdateRequest request) {
        Resume resume = resumeRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("이력서를 찾을 수 없습니다."));

        // 현재 내용을 버전 스냅샷으로 저장
        if (resume.getContent() != null) {
            ResumeVersion version = ResumeVersion.create(
                    resume.getId(),
                    resume.getCurrentVersion(),
                    resume.getTitle(),
                    resume.getContent(),
                    request.getMemo()
            );
            versionRepository.save(version);
            log.info("이력서 버전 {} 저장됨", resume.getCurrentVersion());
        }

        // 새 내용으로 업데이트
        resume.update(request.getTitle(), request.getContent());
        
        // 스킬 태그 업데이트
        if (request.getSkills() != null) {
            resume.updateSkills(toJson(request.getSkills()));
        }
        
        resumeRepository.save(resume);

        // AI 서비스에 이력서 저장 (비동기)
        if (request.getContent() != null && !request.getContent().isBlank()) {
            aiServiceClient.saveResumeAsync(
                resume.getId(),
                request.getContent(),
                request.getSkills()
            );
        }

        return ResumeDto.ResumeResponse.from(resume);
    }

    // ============================================
    // 버전 관리
    // ============================================

    /**
     * 버전 목록 조회
     */
    @Transactional(readOnly = true)
    public ResumeDto.VersionListResponse getVersions(Long userId) {
        Resume resume = resumeRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("이력서를 찾을 수 없습니다."));

        List<ResumeVersion> versions = versionRepository.findByResumeIdOrderByVersionDesc(resume.getId());
        List<ResumeDto.VersionResponse> responses = versions.stream()
                .map(ResumeDto.VersionResponse::from)
                .collect(Collectors.toList());

        return ResumeDto.VersionListResponse.builder()
                .versions(responses)
                .totalCount(responses.size())
                .build();
    }

    /**
     * 특정 버전 조회
     */
    @Transactional(readOnly = true)
    public ResumeDto.VersionResponse getVersion(Long userId, int version) {
        Resume resume = resumeRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("이력서를 찾을 수 없습니다."));

        ResumeVersion resumeVersion = versionRepository.findByResumeIdAndVersion(resume.getId(), version)
                .orElseThrow(() -> new IllegalArgumentException("버전을 찾을 수 없습니다."));

        return ResumeDto.VersionResponse.from(resumeVersion);
    }

    /**
     * 특정 버전으로 복원
     */
    @Transactional
    public ResumeDto.ResumeResponse restoreVersion(Long userId, int version) {
        Resume resume = resumeRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("이력서를 찾을 수 없습니다."));

        ResumeVersion targetVersion = versionRepository.findByResumeIdAndVersion(resume.getId(), version)
                .orElseThrow(() -> new IllegalArgumentException("버전을 찾을 수 없습니다."));

        // 현재 내용을 버전 스냅샷으로 저장
        ResumeVersion currentSnapshot = ResumeVersion.create(
                resume.getId(),
                resume.getCurrentVersion(),
                resume.getTitle(),
                resume.getContent(),
                "버전 " + version + "으로 복원 전 백업"
        );
        versionRepository.save(currentSnapshot);

        // 선택한 버전으로 복원
        resume.restoreFromVersion(
                targetVersion.getTitle(),
                targetVersion.getContent(),
                resume.getCurrentVersion() + 1
        );
        resumeRepository.save(resume);

        log.info("이력서를 버전 {}에서 복원, 새 버전: {}", version, resume.getCurrentVersion());
        return ResumeDto.ResumeResponse.from(resume);
    }

    // ============================================
    // AI 분석
    // ============================================

    /**
     * AI 분석 요청 (비동기로 분석 후 결과 저장)
     */
    @Transactional
    public ResumeDto.ResumeResponse requestAnalysis(Long userId) {
        Resume resume = resumeRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("이력서를 찾을 수 없습니다."));

        if (resume.getContent() == null || resume.getContent().isBlank()) {
            throw new IllegalStateException("이력서 내용이 없습니다.");
        }

        // 상태를 ANALYZING으로 변경
        resume.markAnalyzing();
        resumeRepository.save(resume);

        final Long resumeId = resume.getId();
        final String content = resume.getContent();

        // 비동기로 AI 분석 수행 (완료 후 자동 저장)
        aiServiceClient.analyzeAndSaveAsync(resumeId, content);

        log.info("AI 분석 요청: resumeId={}", resumeId);
        return ResumeDto.ResumeResponse.from(resume);
    }

    /**
     * AI 분석 결과 저장 (외부 콜백용 - 기존 메서드 유지)
     */
    @Transactional
    public ResumeDto.ResumeResponse updateAiResult(Long resumeId, ResumeDto.AiResultRequest request) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("이력서를 찾을 수 없습니다."));

        resume.updateAiAnalysis(
                request.getSummary(),
                request.getSkills()
        );
        resumeRepository.save(resume);

        log.info("AI 분석 결과 저장: resumeId={}", resumeId);
        return ResumeDto.ResumeResponse.from(resume);
    }

    // ============================================
    // Private Methods
    // ============================================

    /**
     * 스킬 리스트를 JSON 배열 문자열로 변환
     */
    private String toJson(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return null;
        }
        return "[\"" + String.join("\",\"", skills) + "\"]";
    }
}
