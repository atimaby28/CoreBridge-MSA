package halo.corebridge.jobpostingread.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class ViewClient {

    private final RestTemplate restTemplate;

    @Value("${client.view.url:http://localhost:8004}")
    private String viewServiceUrl;

    public Long count(Long jobpostingId) {
        try {
            String url = viewServiceUrl + "/api/v1/jobposting-views/jobpostings/" + jobpostingId + "/count";
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