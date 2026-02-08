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
public class UserClient {

    private final RestTemplate restTemplate;

    @Value("${client.user.url:http://localhost:8001}")
    private String userServiceUrl;

    @CircuitBreaker(name = "userService", fallbackMethod = "readFallback")
    public UserResponse read(Long userId) {
        if (userId == null) {
            return null;
        }
        String url = userServiceUrl + "/api/v1/users/" + userId;
        BaseResponse response = restTemplate.getForObject(url, BaseResponse.class);
        return response != null ? response.getResult() : null;
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getNicknameFallback")
    public String getNickname(Long userId) {
        if (userId == null) {
            return "익명";
        }
        String url = userServiceUrl + "/api/v1/users/" + userId;
        BaseResponse response = restTemplate.getForObject(url, BaseResponse.class);
        if (response != null && response.getResult() != null) {
            return response.getResult().getNickname();
        }
        return "익명";
    }

    // ===== Fallback Methods =====

    private UserResponse readFallback(Long userId, Throwable t) {
        log.warn("[CircuitBreaker] userService.read FALLBACK - userId={}, error={}", userId, t.getMessage());
        return null;
    }

    private String getNicknameFallback(Long userId, Throwable t) {
        log.warn("[CircuitBreaker] userService.getNickname FALLBACK - userId={}, error={}", userId, t.getMessage());
        return "익명";
    }

    @lombok.Data
    public static class BaseResponse {
        private boolean success;
        private int code;
        private String message;
        private UserResponse result;
    }

    @lombok.Data
    public static class UserResponse {
        private Long userId;
        private String email;
        private String nickname;
    }
}
