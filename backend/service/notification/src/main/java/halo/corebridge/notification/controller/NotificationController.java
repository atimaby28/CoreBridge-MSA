package halo.corebridge.notification.controller;

import halo.corebridge.common.response.BaseResponse;
import halo.corebridge.notification.model.dto.NotificationDto;
import halo.corebridge.notification.service.NotificationService;
import halo.corebridge.notification.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 외부 API - JWT 인증 필요
 * 프론트엔드에서 호출
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;

    /**
     * SSE 구독 — 실시간 알림 수신
     * Gateway 경유 시: @AuthenticationPrincipal로 userId 추출
     * 직접 연결 시: ?userId= 쿼리 파라미터로 userId 전달
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @AuthenticationPrincipal Long authUserId,
            @RequestParam(value = "userId", required = false) Long queryUserId) {
        Long userId = authUserId != null ? authUserId : queryUserId;
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        return sseEmitterService.subscribe(userId);
    }

    /**
     * 내 알림 목록 조회 (페이징)
     */
    @GetMapping
    public BaseResponse<Page<NotificationDto.Response>> getMyNotifications(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return BaseResponse.success(notificationService.getMyNotifications(userId, pageable));
    }

    /**
     * 읽지 않은 알림만 조회
     */
    @GetMapping("/unread")
    public BaseResponse<Page<NotificationDto.Response>> getUnreadNotifications(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return BaseResponse.success(notificationService.getUnreadNotifications(userId, pageable));
    }

    /**
     * 읽지 않은 알림 개수 (Polling용)
     */
    @GetMapping("/unread-count")
    public BaseResponse<NotificationDto.UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal Long userId) {
        return BaseResponse.success(notificationService.getUnreadCount(userId));
    }

    /**
     * 최근 알림 10개 조회 (Polling용 - 빠른 응답)
     */
    @GetMapping("/recent")
    public BaseResponse<List<NotificationDto.Response>> getRecentNotifications(
            @AuthenticationPrincipal Long userId) {
        return BaseResponse.success(notificationService.getRecentNotifications(userId));
    }

    /**
     * 알림 단건 조회
     */
    @GetMapping("/{id}")
    public BaseResponse<NotificationDto.Response> getNotification(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        return BaseResponse.success(notificationService.getNotification(userId, id));
    }

    /**
     * 알림 단건 읽음 처리
     */
    @PatchMapping("/{id}/read")
    public BaseResponse<Boolean> markAsRead(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        return BaseResponse.success(notificationService.markAsRead(userId, id));
    }

    /**
     * 모든 알림 읽음 처리
     */
    @PatchMapping("/read-all")
    public BaseResponse<Integer> markAllAsRead(@AuthenticationPrincipal Long userId) {
        return BaseResponse.success(notificationService.markAllAsRead(userId));
    }
}