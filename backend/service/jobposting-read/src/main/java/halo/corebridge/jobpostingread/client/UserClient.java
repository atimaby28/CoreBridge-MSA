package halo.corebridge.jobpostingread.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class UserClient {

    private final RestTemplate restTemplate;

    @Value("${client.user.url:http://localhost:8001}")
    private String userServiceUrl;

    public UserResponse read(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            String url = userServiceUrl + "/api/v1/users/" + userId;
            BaseResponse response = restTemplate.getForObject(url, BaseResponse.class);
            return response != null ? response.getResult() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public String getNickname(Long userId) {
        UserResponse user = read(userId);
        return user != null ? user.getNickname() : "익명";
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