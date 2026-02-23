package halo.corebridge.jobpostinghot.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ViewClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${client.view.url:http://localhost:8004}")
    private String viewServiceUrl;

    public Long count(Long jobpostingId) {
        try {
            String url = viewServiceUrl + "/api/v1/jobposting-views/jobpostings/" + jobpostingId + "/count";
            
            // BaseResponse 형태로 받기
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getBody() != null && response.getBody().get("result") != null) {
                Object result = response.getBody().get("result");
                if (result instanceof Number) {
                    return ((Number) result).longValue();
                }
            }
            return 0L;
        } catch (Exception e) {
            log.warn("Failed to fetch view count for jobposting: {}, error: {}", jobpostingId, e.getMessage());
            return 0L;
        }
    }
}
