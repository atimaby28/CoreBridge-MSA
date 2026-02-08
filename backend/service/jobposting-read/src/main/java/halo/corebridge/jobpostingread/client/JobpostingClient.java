package halo.corebridge.jobpostingread.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobpostingClient {

    private final RestTemplate restTemplate;

    @Value("${client.jobposting.url:http://localhost:8002}")
    private String jobpostingServiceUrl;

    @CircuitBreaker(name = "jobpostingService", fallbackMethod = "readFallback")
    public JobpostingResponse read(Long jobpostingId) {
        String url = jobpostingServiceUrl + "/api/v1/jobpostings/" + jobpostingId;
        BaseResponse response = restTemplate.getForObject(url, BaseResponse.class);
        return response != null ? response.getResult() : null;
    }

    @CircuitBreaker(name = "jobpostingService", fallbackMethod = "readAllFallback")
    public JobpostingPageResponse readAll(Long boardId, Long page, Long pageSize) {
        String url = String.format("%s/api/v1/jobpostings?boardId=%d&page=%d&pageSize=%d",
                jobpostingServiceUrl, boardId, page, pageSize);
        BasePageResponse response = restTemplate.getForObject(url, BasePageResponse.class);
        return response != null ? response.getResult() : null;
    }

    @CircuitBreaker(name = "jobpostingService", fallbackMethod = "countFallback")
    public Long count(Long boardId) {
        String url = jobpostingServiceUrl + "/api/v1/jobpostings/boards/" + boardId + "/count";
        BaseCountResponse response = restTemplate.getForObject(url, BaseCountResponse.class);
        return response != null && response.getResult() != null ? response.getResult() : 0L;
    }

    // ===== Fallback Methods =====

    private JobpostingResponse readFallback(Long jobpostingId, Throwable t) {
        log.warn("[CircuitBreaker] jobpostingService.read FALLBACK - jobpostingId={}, error={}", jobpostingId, t.getMessage());
        return null;
    }

    private JobpostingPageResponse readAllFallback(Long boardId, Long page, Long pageSize, Throwable t) {
        log.warn("[CircuitBreaker] jobpostingService.readAll FALLBACK - boardId={}, error={}", boardId, t.getMessage());
        return null;
    }

    private Long countFallback(Long boardId, Throwable t) {
        log.warn("[CircuitBreaker] jobpostingService.count FALLBACK - boardId={}, error={}", boardId, t.getMessage());
        return 0L;
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
