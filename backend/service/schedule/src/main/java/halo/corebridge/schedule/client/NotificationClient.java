package halo.corebridge.schedule.client;

import halo.corebridge.schedule.model.entity.Schedule;
import halo.corebridge.schedule.model.enums.ScheduleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationClient {

    private final RestTemplate restTemplate;

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm");

    /**
     * 일정 등록 알림 전송
     */
    @Async
    public void sendScheduleCreatedNotification(Schedule schedule) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("userId", schedule.getUserId());
            request.put("type", getNotificationType(schedule.getType()));
            request.put("title", getTitle(schedule.getType()));
            request.put("message", getMessage(schedule));
            request.put("link", "/schedules/" + schedule.getId());
            request.put("relatedId", schedule.getId());
            request.put("relatedType", "SCHEDULE");

            restTemplate.postForEntity(
                    notificationServiceUrl + "/internal/v1/notifications",
                    request,
                    Object.class
            );

            log.info("일정 알림 전송 성공: userId={}, type={}", schedule.getUserId(), schedule.getType());
        } catch (Exception e) {
            log.error("일정 알림 전송 실패: userId={}, error={}", schedule.getUserId(), e.getMessage());
        }
    }

    /**
     * 일정 변경 알림 전송
     */
    @Async
    public void sendScheduleUpdatedNotification(Schedule schedule) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("userId", schedule.getUserId());
            request.put("type", "SCHEDULE_CHANGED");
            request.put("title", "일정이 변경되었습니다");
            request.put("message", String.format("%s 일정이 변경되었습니다. %s",
                    schedule.getType().getDescription(),
                    schedule.getStartTime().format(DATE_FORMATTER)));
            request.put("link", "/schedules/" + schedule.getId());
            request.put("relatedId", schedule.getId());
            request.put("relatedType", "SCHEDULE");

            restTemplate.postForEntity(
                    notificationServiceUrl + "/internal/v1/notifications",
                    request,
                    Object.class
            );

            log.info("일정 변경 알림 전송 성공: userId={}", schedule.getUserId());
        } catch (Exception e) {
            log.error("일정 변경 알림 전송 실패: userId={}, error={}", schedule.getUserId(), e.getMessage());
        }
    }

    /**
     * 일정 취소 알림 전송
     */
    @Async
    public void sendScheduleCancelledNotification(Schedule schedule) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("userId", schedule.getUserId());
            request.put("type", "SCHEDULE_CANCELLED");
            request.put("title", "일정이 취소되었습니다");
            request.put("message", String.format("%s 일정이 취소되었습니다.",
                    schedule.getType().getDescription()));
            request.put("link", "/applies/" + schedule.getApplyId());
            request.put("relatedId", schedule.getId());
            request.put("relatedType", "SCHEDULE");

            restTemplate.postForEntity(
                    notificationServiceUrl + "/internal/v1/notifications",
                    request,
                    Object.class
            );

            log.info("일정 취소 알림 전송 성공: userId={}", schedule.getUserId());
        } catch (Exception e) {
            log.error("일정 취소 알림 전송 실패: userId={}, error={}", schedule.getUserId(), e.getMessage());
        }
    }

    private String getNotificationType(ScheduleType type) {
        return switch (type) {
            case CODING_TEST -> "CODING_TEST_SCHEDULED";
            case INTERVIEW_1, INTERVIEW_2, FINAL_INTERVIEW -> "INTERVIEW_SCHEDULED";
            case ORIENTATION -> "ORIENTATION_SCHEDULED";
            case OTHER -> "SCHEDULE_CREATED";
        };
    }

    private String getTitle(ScheduleType type) {
        return switch (type) {
            case CODING_TEST -> "코딩 테스트 일정이 등록되었습니다";
            case INTERVIEW_1 -> "1차 면접 일정이 등록되었습니다";
            case INTERVIEW_2 -> "2차 면접 일정이 등록되었습니다";
            case FINAL_INTERVIEW -> "최종 면접 일정이 등록되었습니다";
            case ORIENTATION -> "오리엔테이션 일정이 등록되었습니다";
            case OTHER -> "새로운 일정이 등록되었습니다";
        };
    }

    private String getMessage(Schedule schedule) {
        String dateTime = schedule.getStartTime().format(DATE_FORMATTER);
        String location = schedule.getLocation() != null ? schedule.getLocation() : "추후 안내";

        return String.format("%s에 %s이(가) 예정되어 있습니다. 장소: %s",
                dateTime,
                schedule.getType().getDescription(),
                location);
    }
}
