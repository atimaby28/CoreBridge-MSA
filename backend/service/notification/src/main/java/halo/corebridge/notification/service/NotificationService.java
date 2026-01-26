package halo.corebridge.notification.service;

import halo.corebridge.notification.model.dto.NotificationDto;
import halo.corebridge.notification.model.entity.Notification;
import halo.corebridge.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // ===================================================================
    // SSE 전환 시 추가할 부분 (현재는 주석 처리)
    // ===================================================================
    // private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    //
    // public SseEmitter subscribe(Long userId) {
    //     SseEmitter emitter = new SseEmitter(60 * 60 * 1000L); // 1시간
    //     emitters.put(userId, emitter);
    //
    //     emitter.onCompletion(() -> emitters.remove(userId));
    //     emitter.onTimeout(() -> emitters.remove(userId));
    //     emitter.onError((e) -> emitters.remove(userId));
    //
    //     // 연결 시 읽지 않은 알림 개수 전송
    //     sendToUser(userId, "connect", getUnreadCount(userId));
    //
    //     return emitter;
    // }
    //
    // private void sendToUser(Long userId, String eventName, Object data) {
    //     SseEmitter emitter = emitters.get(userId);
    //     if (emitter != null) {
    //         try {
    //             emitter.send(SseEmitter.event()
    //                     .name(eventName)
    //                     .data(data));
    //         } catch (IOException e) {
    //             emitters.remove(userId);
    //         }
    //     }
    // }
    // ===================================================================

    /**
     * 알림 생성 (내부 서비스에서 호출)
     */
    @Transactional
    public NotificationDto.CreateResponse create(NotificationDto.CreateRequest request) {
        try {
            Notification notification = request.toEntity();
            Notification saved = notificationRepository.save(notification);

            log.info("알림 생성 완료: userId={}, type={}, id={}",
                    request.getUserId(), request.getType(), saved.getId());

            // SSE 전환 시: 실시간 푸시
            // sendToUser(request.getUserId(), "notification", NotificationDto.Response.from(saved));

            return NotificationDto.CreateResponse.success(saved.getId());
        } catch (Exception e) {
            log.error("알림 생성 실패: userId={}, type={}, error={}",
                    request.getUserId(), request.getType(), e.getMessage());
            return NotificationDto.CreateResponse.fail(e.getMessage());
        }
    }

    /**
     * 내 알림 목록 조회 (페이징)
     */
    public Page<NotificationDto.Response> getMyNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationDto.Response::from);
    }

    /**
     * 읽지 않은 알림만 조회
     */
    public Page<NotificationDto.Response> getUnreadNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationDto.Response::from);
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    public NotificationDto.UnreadCountResponse getUnreadCount(Long userId) {
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return NotificationDto.UnreadCountResponse.of(count);
    }

    /**
     * 최근 알림 10개 조회 (Polling용 - 빠른 응답)
     */
    public List<NotificationDto.Response> getRecentNotifications(Long userId) {
        return notificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationDto.Response::from)
                .toList();
    }

    /**
     * 알림 단건 읽음 처리
     */
    @Transactional
    public boolean markAsRead(Long userId, Long notificationId) {
        return notificationRepository.findById(notificationId)
                .filter(n -> n.getUserId().equals(userId))
                .map(notification -> {
                    notification.markAsRead();
                    log.debug("알림 읽음 처리: userId={}, notificationId={}", userId, notificationId);
                    return true;
                })
                .orElse(false);
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        int count = notificationRepository.markAllAsReadByUserId(userId);
        log.info("모든 알림 읽음 처리: userId={}, count={}", userId, count);
        return count;
    }

    /**
     * 알림 단건 조회
     */
    public NotificationDto.Response getNotification(Long userId, Long notificationId) {
        return notificationRepository.findById(notificationId)
                .filter(n -> n.getUserId().equals(userId))
                .map(NotificationDto.Response::from)
                .orElse(null);
    }
}
