package halo.corebridge.jobpostingread.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JobpostingClient {

    private final RestTemplate restTemplate;

    @Value("${client.jobposting.url:http://localhost:8002}")
    private String jobpostingServiceUrl;

    public JobpostingResponse read(Long jobpostingId) {
        String url = jobpostingServiceUrl + "/api/v1/jobpostings/" + jobpostingId;
        BaseResponse response = restTemplate.getForObject(url, BaseResponse.class);
        return response != null ? response.getResult() : null;
    }

    public JobpostingPageResponse readAll(Long boardId, Long page, Long pageSize) {
        String url = String.format("%s/api/v1/jobpostings?boardId=%d&page=%d&pageSize=%d",
                jobpostingServiceUrl, boardId, page, pageSize);
        BasePageResponse response = restTemplate.getForObject(url, BasePageResponse.class);
        return response != null ? response.getResult() : null;
    }

    public Long count(Long boardId) {
        String url = jobpostingServiceUrl + "/api/v1/jobpostings/boards/" + boardId + "/count";
        try {
            BaseCountResponse response = restTemplate.getForObject(url, BaseCountResponse.class);
            return response != null && response.getResult() != null ? response.getResult() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    // BaseResponse Wrappers
    @lombok.Data
    public static class BaseResponse {
        private boolean success;
        private int code;
        private String message;
        private JobpostingResponse result;
    }

    @lombok.Data
    public static class BasePageResponse {
        private boolean success;
        private int code;
        private String message;
        private JobpostingPageResponse result;
    }

    @lombok.Data
    public static class BaseCountResponse {
        private boolean success;
        private int code;
        private String message;
        private Long result;
    }

    // Inner Response Classes
    @lombok.Data
    public static class JobpostingResponse {
        private Long jobpostingId;
        private String title;
        private String content;
        private Long boardId;
        private Long userId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @lombok.Data
    public static class JobpostingPageResponse {
        private List<JobpostingResponse> jobpostings;
        private Long jobpostingCount;
    }
}