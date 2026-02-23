package halo.corebridge.jobpostinghot.client;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobpostingClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${client.jobposting.url:http://localhost:8001}")
    private String jobpostingServiceUrl;

    /**
     * 단일 채용공고 조회
     */
    public JobpostingResponse read(Long jobpostingId) {
        try {
            String url = jobpostingServiceUrl + "/api/v1/jobpostings/" + jobpostingId;
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, 
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> body = response.getBody();
            if (body != null && body.get("result") != null) {
                Map<String, Object> result = (Map<String, Object>) body.get("result");
                return mapToJobpostingResponse(result);
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch jobposting: {}", jobpostingId, e);
            return null;
        }
    }

    /**
     * 게시판별 채용공고 목록 조회
     */
    public JobpostingPageResponse readAll(Long boardId, Long page, Long pageSize) {
        try {
            String url = String.format("%s/api/v1/jobpostings?boardId=%d&page=%d&pageSize=%d",
                    jobpostingServiceUrl, boardId, page, pageSize);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, 
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> body = response.getBody();
            if (body != null && body.get("result") != null) {
                Map<String, Object> result = (Map<String, Object>) body.get("result");
                return mapToPageResponse(result);
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch jobpostings: boardId={}", boardId, e);
            return null;
        }
    }

    private JobpostingResponse mapToJobpostingResponse(Map<String, Object> map) {
        JobpostingResponse response = new JobpostingResponse();
        response.setJobpostingId(getLong(map, "jobpostingId"));
        response.setTitle((String) map.get("title"));
        response.setContent((String) map.get("content"));
        response.setBoardId(getLong(map, "boardId"));
        response.setUserId(getLong(map, "userId"));
        return response;
    }

    private JobpostingPageResponse mapToPageResponse(Map<String, Object> map) {
        JobpostingPageResponse pageResponse = new JobpostingPageResponse();
        
        List<Map<String, Object>> jobpostings = (List<Map<String, Object>>) map.get("jobpostings");
        if (jobpostings != null) {
            pageResponse.setJobpostings(
                jobpostings.stream()
                    .map(this::mapToJobpostingResponse)
                    .toList()
            );
        }
        pageResponse.setJobpostingCount(getLong(map, "jobpostingCount"));
        return pageResponse;
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof String) return Long.parseLong((String) value);
        return null;
    }

    @Data
    public static class JobpostingResponse {
        private Long jobpostingId;
        private String title;
        private String content;
        private Long boardId;
        private Long userId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class JobpostingPageResponse {
        private List<JobpostingResponse> jobpostings;
        private Long jobpostingCount;
    }
}
