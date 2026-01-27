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
     * JD에 맞는 후보자 매칭 (벡터 검색)
     */
    public List<AiMatchingDto.MatchedCandidate> matchCandidates(String jdText, int topK) {
        try {
            log.info("[AI Matching] 후보자 매칭 시작: topK={}", topK);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("jd_text", jdText);
            body.put("top_k", topK);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            String url = aiServiceUrl + "/match_jd";
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response != null && response.containsKey("matches")) {
                List<Map<String, Object>> matches = (List<Map<String, Object>>) response.get("matches");
                List<AiMatchingDto.MatchedCandidate> candidates = new ArrayList<>();

                for (Map<String, Object> match : matches) {
                    candidates.add(AiMatchingDto.MatchedCandidate.builder()
                            .candidateId(String.valueOf(match.get("candidate_id")))
                            .score(((Number) match.get("score")).doubleValue())
                            .build());
                }

                log.info("[AI Matching] 후보자 매칭 완료: {} 명", candidates.size());
                return candidates;
            }

            return List.of();

        } catch (Exception e) {
            log.error("[AI Matching] 후보자 매칭 실패: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 특정 후보자의 상세 스코어 계산
     */
    public AiMatchingDto.ScoreResponse scoreCandidate(String candidateId, String jdText, List<String> requiredSkills) {
        try {
            log.info("[AI Matching] 스코어 계산 시작: candidateId={}", candidateId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("candidate_id", candidateId);
            body.put("jd_text", jdText);
            if (requiredSkills != null && !requiredSkills.isEmpty()) {
                body.put("required_skills", requiredSkills);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            String url = aiServiceUrl + "/score";
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

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
}
