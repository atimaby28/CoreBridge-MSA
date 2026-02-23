package halo.corebridge.notification.controller;

import halo.corebridge.notification.model.dto.NotificationDto;
import halo.corebridge.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 내부 API - 인증 불필요
 * 다른 마이크로서비스에서 호출 (apply, resume 등)
 */
@RestController
@RequestMapping("/internal/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class InternalNotificationController {

    private final NotificationService notificationService;

    /**
     * 알림 생성 (다른 서비스에서 호출)
     *
     * 사용 예시:
     * - Apply 서비스: 채용 프로세스 상태 변경 시
     * - Resume 서비스: AI 분석 완료 시
     */
    @PostMapping
    public ResponseEntity<NotificationDto.CreateResponse> createNotification(
            @Valid @RequestBody NotificationDto.CreateRequest request) {

        log.info("내부 알림 생성 요청: userId={}, type={}", request.getUserId(), request.getType());

        NotificationDto.CreateResponse response = notificationService.create(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 헬스체크 (서비스 간 연결 확인용)
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
