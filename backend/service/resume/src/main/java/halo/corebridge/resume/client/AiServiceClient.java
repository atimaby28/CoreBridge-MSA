package halo.corebridge.resume.client;

import halo.corebridge.resume.service.AiAnalysisResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiServiceClient {

    private final RestTemplate restTemplate;
    private final AiAnalysisResultService aiAnalysisResultService;

    @Value("${ai.service.url:http://localhost:9001}")
    private String aiServiceUrl;

    /**
     * AI 서비스에 이력서 저장 (비동기)
     * - 임베딩 생성 및 Redis Vector DB 저장
     * - 스킬 태그 함께 저장
     */
    @Async("aiServiceExecutor")
    public void saveResumeAsync(Long resumeId, String resumeText, List<String> skills) {
        try {
            log.info("[AI Service] 이력서 저장 요청 시작: resumeId={}", resumeId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("candidate_id", String.valueOf(resumeId));
            body.put("resume_text", resumeText);
            if (skills != null && !skills.isEmpty()) {
                body.put("skills", skills);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            String url = aiServiceUrl + "/save_resume";
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            log.info("[AI Service] 이력서 저장 완료: resumeId={}, response={}", resumeId, response);

        } catch (Exception e) {
            log.error("[AI Service] 이력서 저장 실패: resumeId={}, error={}", resumeId, e.getMessage());
        }
    }

    /**
     * 텍스트 요약 (동기)
     */
    public String summarize(String text) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("text", text);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            String url = aiServiceUrl + "/summary";
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response != null && response.containsKey("summary")) {
                return (String) response.get("summary");
            }
        } catch (Exception e) {
            log.error("[AI Service] 요약 실패: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 스킬 추출 (동기)
     */
    public List<String> extractSkills(String text) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("text", text);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            String url = aiServiceUrl + "/skills";
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response != null && response.containsKey("skills")) {
                return (List<String>) response.get("skills");
            }
        } catch (Exception e) {
            log.error("[AI Service] 스킬 추출 실패: {}", e.getMessage());
        }
        return List.of();
    }

    /**
     * AI 분석 결과 DTO
     */
    public record AnalysisResult(String summary, List<String> skills) {}

    /**
     * AI 분석 수행 (비동기) - 요약 + 스킬 추출 후 결과 저장
     */
    @Async("aiServiceExecutor")
    public void analyzeAndSaveAsync(Long resumeId, String resumeText) {
        try {
            log.info("[AI Service] 분석 시작: resumeId={}", resumeId);

            // 1. 요약 생성
            String summary = summarize(resumeText);
            
            // 2. 스킬 추출
            List<String> skills = extractSkills(resumeText);

            log.info("[AI Service] 분석 완료: resumeId={}, skills={}", resumeId, skills);

            // 3. 결과 저장 (별도 서비스 호출 - 트랜잭션 적용됨)
            aiAnalysisResultService.saveResult(resumeId, summary, skills);

        } catch (Exception e) {
            log.error("[AI Service] 분석 실패: resumeId={}, error={}", resumeId, e.getMessage());
        }
    }
}
