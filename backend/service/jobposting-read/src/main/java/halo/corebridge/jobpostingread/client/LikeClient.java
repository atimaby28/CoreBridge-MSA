package halo.corebridge.jobpostingread.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeClient {

    private final RestTemplate restTemplate;

    @Value("${client.like.url:http://localhost:8005}")
    private String likeServiceUrl;

    @CircuitBreaker(name = "likeService", fallbackMethod = "countFallback")
    public Long count(Long jobpostingId) {
        String url = likeServiceUrl + "/api/v1/jobposting-likes/jobpostings/" + jobpostingId + "/count";
        BaseResponse response = restTemplate.getForObject(url, BaseResponse.class);
        return response != null && response.getResult() != null ? response.getResult() : 0L;
    }

    // ===== Fallback Methods =====

    private Long countFallback(Long jobpostingId, Throwable t) {
        log.warn("[CircuitBreaker] likeService.count FALLBACK - jobpostingId={}, error={}", jobpostingId, t.getMessage());
        return 0L;
    }

    @lombok.Data
    public static class BaseResponse {
        private boolean success;
        private int code;
        private String message;
        private Long result;
    }
}
