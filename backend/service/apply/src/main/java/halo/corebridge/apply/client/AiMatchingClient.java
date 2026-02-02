package halo.corebridge.apply.client;

import halo.corebridge.apply.model.dto.AiMatchingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiMatchingClient {

    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:9001}")
    private String aiServiceUrl;

    /**
     * JD에 맞는 후보자 매칭 (회사용 - 벡터 검색)
     */
    public List<AiMatchingDto.MatchedCandidate> matchCandidates(String jdText, int topK) {
        try {
            log.info("[AI Matching] 후보자 매칭 시작: topK={}", topK);

            Map<String, Object> body = new HashMap<>();
            body.put("jd_text", jdText);
            body.put("top_k", topK);

            Map<String, Object> response = post("/match_candidates", body);

            if (response != null && response.containsKey("matches")) {
                List<Map<String, Object>> matches = (List<Map<String, Object>>) response.get("matches");
                List<AiMatchingDto.MatchedCandidate> candidates = new ArrayList<>();

                for (Map<String, Object> match : matches) {
                    candidates.add(AiMatchingDto.MatchedCandidate.builder()
                            .candidateId(String.valueOf(match.get("candidate_id")))
                            .score(((Number) match.get("score")).doubleValue())
                            .build());
                }

                log.info("[AI Matching] 후보자 매칭 완료: {}명", candidates.size());
                return candidates;
            }

            return List.of();

        } catch (Exception e) {
            log.error("[AI Matching] 후보자 매칭 실패: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 이력서에 맞는 채용공고 매칭 (구직자용 - 벡터 검색)
     */
    public List<AiMatchingDto.MatchedJobposting> matchJobpostings(String resumeText, int topK) {
        try {
            log.info("[AI Matching] 채용공고 매칭 시작: topK={}", topK);

            Map<String, Object> body = new HashMap<>();
            body.put("resume_text", resumeText);
            body.put("top_k", topK);

            Map<String, Object> response = post("/match_jobpostings", body);

            if (response != null && response.containsKey("matches")) {
                List<Map<String, Object>> matches = (List<Map<String, Object>>) response.get("matches");
                List<AiMatchingDto.MatchedJobposting> jobpostings = new ArrayList<>();

                for (Map<String, Object> match : matches) {
                    jobpostings.add(AiMatchingDto.MatchedJobposting.builder()
                            .jobpostingId(String.valueOf(match.get("jobposting_id")))
                            .score(((Number) match.get("score")).doubleValue())
                            .build());
                }

                log.info("[AI Matching] 채용공고 매칭 완료: {}건", jobpostings.size());
                return jobpostings;
            }

            return List.of();

        } catch (Exception e) {
            log.error("[AI Matching] 채용공고 매칭 실패: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 특정 후보자의 상세 스코어 계산
     */
    public AiMatchingDto.ScoreResponse scoreCandidate(String candidateId, String jdText, List<String> requiredSkills) {
        try {
            log.info("[AI Matching] 스코어 계산 시작: candidateId={}", candidateId);

            Map<String, Object> body = new HashMap<>();
            body.put("candidate_id", candidateId);
            body.put("jd_text", jdText);
            if (requiredSkills != null && !requiredSkills.isEmpty()) {
                body.put("required_skills", requiredSkills);
            }

            Map<String, Object> response = post("/score", body);

            if (response != null) {
                Map<String, Object> scoreDetail = (Map<String, Object>) response.get("score_detail");

                return AiMatchingDto.ScoreResponse.builder()
                        .candidateId(String.valueOf(response.get("candidate_id")))
                        .requiredSkills((List<String>) response.get("required_skills"))
                        .candidateSkills((List<String>) response.get("candidate_skills"))
                        .cosineSimilarity(((Number) response.get("cosine_similarity")).doubleValue())
                        .scoreDetail(AiMatchingDto.ScoreDetail.builder()
                                .skillScore(((Number) scoreDetail.get("skill_score")).doubleValue())
                                .similarityScore(((Number) scoreDetail.get("similarity_score")).doubleValue())
                                .bonusScore(((Number) scoreDetail.get("bonus_score")).doubleValue())
                                .totalScore(((Number) scoreDetail.get("total_score")).doubleValue())
                                .grade((String) scoreDetail.get("grade"))
                                .build())
                        .build();
            }

            return null;

        } catch (Exception e) {
            log.error("[AI Matching] 스코어 계산 실패: candidateId={}, error={}", candidateId, e.getMessage());
            return null;
        }
    }

    /**
     * 스킬 갭 분석 (구직자용)
     */
    public AiMatchingDto.SkillGapResponse analyzeSkillGap(String candidateId, String jobpostingId) {
        try {
            log.info("[AI Matching] 스킬 갭 분석: candidateId={}, jobpostingId={}", candidateId, jobpostingId);

            Map<String, Object> body = new HashMap<>();
            body.put("candidate_id", candidateId);
            body.put("jobposting_id", jobpostingId);

            Map<String, Object> response = post("/skill_gap", body);

            if (response != null) {
                return AiMatchingDto.SkillGapResponse.builder()
                        .candidateId(String.valueOf(response.get("candidate_id")))
                        .jobpostingId(String.valueOf(response.get("jobposting_id")))
                        .candidateSkills((List<String>) response.get("candidate_skills"))
                        .requiredSkills((List<String>) response.get("required_skills"))
                        .matchedSkills((List<String>) response.get("matched_skills"))
                        .missingSkills((List<String>) response.get("missing_skills"))
                        .matchRate(((Number) response.get("match_rate")).doubleValue())
                        .cosineSimilarity(((Number) response.get("cosine_similarity")).doubleValue())
                        .build();
            }

            return null;

        } catch (Exception e) {
            log.error("[AI Matching] 스킬 갭 분석 실패: {}", e.getMessage());
            return null;
        }
    }

    // ============================================
    // Private
    // ============================================

    private Map<String, Object> post(String path, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        return restTemplate.postForObject(aiServiceUrl + path, request, Map.class);
    }
}
