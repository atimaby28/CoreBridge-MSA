package halo.corebridge.jobposting.client;

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
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiServiceClient {

    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:9001}")
    private String aiServiceUrl;

    /**
     * AI 서비스에 채용공고 저장 (비동기)
     * - 임베딩 생성 및 Redis Vector DB 저장
     */
    @Async("aiServiceExecutor")
    public void saveJobpostingAsync(Long jobpostingId, String jobpostingText) {
        try {
            log.info("[AI Service] 채용공고 저장 요청: jobpostingId={}", jobpostingId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("jobposting_id", String.valueOf(jobpostingId));
            body.put("jobposting_text", jobpostingText);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            String url = aiServiceUrl + "/save_jobposting";
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            log.info("[AI Service] 채용공고 저장 완료: jobpostingId={}, response={}", jobpostingId, response);

        } catch (Exception e) {
            log.error("[AI Service] 채용공고 저장 실패: jobpostingId={}, error={}", jobpostingId, e.getMessage());
        }
    }
}
