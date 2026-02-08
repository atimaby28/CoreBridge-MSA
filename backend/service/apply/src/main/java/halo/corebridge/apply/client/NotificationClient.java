package halo.corebridge.apply.client;

import halo.corebridge.apply.model.enums.ProcessStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Notification 서비스 호출 클라이언트
 *
 * 채용 프로세스 상태 변경 시 지원자에게 알림을 전송합니다.
 * 비동기로 처리하여 메인 로직에 영향을 주지 않습니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationClient {

    private final RestTemplate restTemplate;

    @Value("${notification.service.url:http://localhost:8010}")
    private String notificationServiceUrl;

    private static final String API_PATH = "/internal/v1/notifications";

    /**
     * 상태 변경 알림 전송 (비동기)
     */
    @Async
    public void sendProcessNotification(Long userId, ProcessStep toStep, Long applyId, Long jobpostingId) {
        try {
            String notificationType = mapStepToNotificationType(toStep);
            if (notificationType == null) {
                log.debug("알림 대상이 아닌 상태: {}", toStep);
                return;
            }

            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            request.put("type", notificationType);
            request.put("title", generateTitle(toStep));
            request.put("message", generateMessage(toStep));
            request.put("link", "/my/applications/" + applyId);
            request.put("relatedId", applyId);
            request.put("relatedType", "APPLY");

            String url = notificationServiceUrl + API_PATH;
            restTemplate.postForObject(url, request, Map.class);

            log.info("알림 전송 성공: userId={}, type={}", userId, notificationType);

        } catch (Exception e) {
            // 알림 실패가 메인 로직에 영향을 주지 않도록 로그만 남김
            log.error("알림 전송 실패: userId={}, step={}, error={}", userId, toStep, e.getMessage());
        }
    }

    /**
     * ProcessStep → NotificationType 매핑
     */
    private String mapStepToNotificationType(ProcessStep step) {
        return switch (step) {
            case DOCUMENT_PASS -> "DOCUMENT_PASS";
            case DOCUMENT_FAIL -> "DOCUMENT_FAIL";
            case CODING_TEST -> "CODING_TEST_SCHEDULED";
            case CODING_PASS -> "CODING_TEST_PASS";
            case CODING_FAIL -> "CODING_TEST_FAIL";
            case INTERVIEW_1, INTERVIEW_2 -> "INTERVIEW_SCHEDULED";
            case INTERVIEW_1_PASS, INTERVIEW_2_PASS -> "INTERVIEW_PASS";
            case INTERVIEW_1_FAIL, INTERVIEW_2_FAIL -> "INTERVIEW_FAIL";
            case FINAL_PASS -> "FINAL_PASS";
            case FINAL_FAIL -> "FINAL_FAIL";
            default -> null; // APPLIED, DOCUMENT_REVIEW 등은 알림 안 함
        };
    }

    /**
     * 알림 제목 생성
     */
    private String generateTitle(ProcessStep step) {
        return switch (step) {
            case DOCUMENT_PASS -> "서류 전형 합격";
            case DOCUMENT_FAIL -> "서류 전형 결과 안내";
            case CODING_TEST -> "코딩 테스트 안내";
            case CODING_PASS -> "코딩 테스트 합격";
            case CODING_FAIL -> "코딩 테스트 결과 안내";
            case INTERVIEW_1 -> "1차 면접 안내";
            case INTERVIEW_2 -> "2차 면접 안내";
            case INTERVIEW_1_PASS -> "1차 면접 합격";
            case INTERVIEW_2_PASS -> "2차 면접 합격";
            case INTERVIEW_1_FAIL, INTERVIEW_2_FAIL -> "면접 결과 안내";
            case FINAL_PASS -> "🎉 최종 합격을 축하합니다!";
            case FINAL_FAIL -> "최종 결과 안내";
            default -> "채용 진행 상태 변경";
        };
    }

    /**
     * 알림 메시지 생성
     */
    private String generateMessage(ProcessStep step) {
        return switch (step) {
            case DOCUMENT_PASS -> "서류 전형에 합격하셨습니다. 다음 전형 안내를 확인해주세요.";
            case DOCUMENT_FAIL -> "서류 전형 결과를 확인해주세요.";
            case CODING_TEST -> "코딩 테스트 일정이 안내되었습니다. 상세 내용을 확인해주세요.";
            case CODING_PASS -> "코딩 테스트에 합격하셨습니다. 다음 전형 안내를 확인해주세요.";
            case CODING_FAIL -> "코딩 테스트 결과를 확인해주세요.";
            case INTERVIEW_1 -> "1차 면접 일정이 안내되었습니다. 상세 내용을 확인해주세요.";
            case INTERVIEW_2 -> "2차 면접 일정이 안내되었습니다. 상세 내용을 확인해주세요.";
            case INTERVIEW_1_PASS -> "1차 면접에 합격하셨습니다. 2차 면접 안내를 확인해주세요.";
            case INTERVIEW_2_PASS -> "2차 면접에 합격하셨습니다. 최종 결과를 기다려주세요.";
            case INTERVIEW_1_FAIL, INTERVIEW_2_FAIL -> "면접 결과를 확인해주세요.";
            case FINAL_PASS -> "최종 합격을 진심으로 축하드립니다! 입사 관련 안내를 확인해주세요.";
            case FINAL_FAIL -> "채용 결과를 확인해주세요. 좋은 기회가 있으시길 바랍니다.";
            default -> "채용 진행 상태가 변경되었습니다.";
        };
    }
}
