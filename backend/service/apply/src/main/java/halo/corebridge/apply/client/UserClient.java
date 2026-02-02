package halo.corebridge.apply.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * User 서비스 호출 클라이언트
 *
 * AI 매칭 결과에서 candidateId(userId)로 사용자 닉네임을 조회합니다.
 */
@Slf4j
@Component
public class UserClient {

    private final RestTemplate restTemplate;

    @Value("${user.service.url:http://localhost:8001}")
    private String userServiceUrl;

    public UserClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * userId로 닉네임 조회
     * @return 닉네임 (조회 실패 시 null)
     */
    public String getNickname(String userId) {
        try {
            String url = userServiceUrl + "/api/v1/users/" + userId;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("result")) {
                Map<String, Object> result = (Map<String, Object>) response.get("result");
                if (result != null && result.containsKey("nickname")) {
                    return (String) result.get("nickname");
                }
            }
        } catch (Exception e) {
            log.debug("사용자 닉네임 조회 실패: userId={}, error={}", userId, e.getMessage());
        }
        return null;
    }
}
