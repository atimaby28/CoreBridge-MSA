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
public class CommentClient {

    private final RestTemplate restTemplate;

    @Value("${client.comment.url:http://localhost:8003}")
    private String commentServiceUrl;

    @CircuitBreaker(name = "commentService", fallbackMethod = "countFallback")
    public Long count(Long jobpostingId) {
        String url = String.format("%s/api/v1/comments?jobpostingId=%d&page=1&pageSize=1",
                commentServiceUrl, jobpostingId);
        BaseResponse response = restTemplate.getForObject(url, BaseResponse.class);
        if (response != null && response.getResult() != null) {
            return response.getResult().getCommentCount();
        }
        return 0L;
    }

    // ===== Fallback Methods =====

    private Long countFallback(Long jobpostingId, Throwable t) {
        log.warn("[CircuitBreaker] commentService.count FALLBACK - jobpostingId={}, error={}", jobpostingId, t.getMessage());
        return 0L;
    }

    @lombok.Data
    public static class BaseResponse {
        private boolean success;
        private int code;
        private String message;
        private CommentPageResponse result;
    }

    @lombok.Data
    public static class CommentPageResponse {
        private Long commentCount;
    }
}
