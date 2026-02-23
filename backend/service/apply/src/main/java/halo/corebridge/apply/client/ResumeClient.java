package halo.corebridge.apply.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Resume 서비스 호출 클라이언트
 *
 * userId로 이력서 정보(resumeId)를 조회합니다.
 */
@Slf4j
@Component
public class ResumeClient {

    private final RestTemplate restTemplate;

    @Value("${resume.service.url:http://localhost:8008}")
    private String resumeServiceUrl;

    public ResumeClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * userId로 resumeId 조회
     * @return resumeId 문자열 (조회 실패 시 null)
     */
    public String getResumeId(String userId) {
        try {
            String url = resumeServiceUrl + "/api/v1/resumes/by-user/" + userId;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("result")) {
                Map<String, Object> result = (Map<String, Object>) response.get("result");
                if (result != null && result.containsKey("resumeId")) {
                    return String.valueOf(result.get("resumeId"));
                }
            }
        } catch (Exception e) {
            log.debug("이력서 조회 실패: userId={}, error={}", userId, e.getMessage());
        }
        return null;
    }
}
