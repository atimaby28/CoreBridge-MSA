package halo.corebridge.jobpostingread.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class LikeClient {

    private final RestTemplate restTemplate;

    @Value("${client.like.url:http://localhost:8005}")
    private String likeServiceUrl;

    public Long count(Long jobpostingId) {
        try {
            String url = likeServiceUrl + "/api/v1/jobposting-likes/jobpostings/" + jobpostingId + "/count";
            BaseResponse response = restTemplate.getForObject(url, BaseResponse.class);
            return response != null && response.getResult() != null ? response.getResult() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    @lombok.Data
    public static class BaseResponse {
        private boolean success;
        private int code;
        private String message;
        private Long result;
    }
}