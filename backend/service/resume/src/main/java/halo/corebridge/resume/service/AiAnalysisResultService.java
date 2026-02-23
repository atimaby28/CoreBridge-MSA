package halo.corebridge.resume.service;

import halo.corebridge.resume.model.entity.Resume;
import halo.corebridge.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI 분석 결과 저장 서비스
 * - 비동기 콜백에서 호출되므로 별도 서비스로 분리 (트랜잭션 적용 위해)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisResultService {

    private final ResumeRepository resumeRepository;

    /**
     * AI 분석 결과 저장
     */
    @Transactional
    public void saveResult(Long resumeId, String summary, List<String> skills) {
        try {
            Resume resume = resumeRepository.findById(resumeId)
                    .orElseThrow(() -> new IllegalArgumentException("이력서를 찾을 수 없습니다."));

            String skillsJson = toJson(skills);
            resume.updateAiAnalysis(summary, skillsJson);
            resumeRepository.save(resume);

            log.info("[AI Analysis] 결과 저장 완료: resumeId={}, skills={}", resumeId, skills);
        } catch (Exception e) {
            log.error("[AI Analysis] 결과 저장 실패: resumeId={}, error={}", resumeId, e.getMessage());
        }
    }

    private String toJson(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return null;
        }
        return "[\"" + String.join("\",\"", skills) + "\"]";
    }
}
