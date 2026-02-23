package halo.corebridge.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import halo.corebridge.notification.config.RedisConfig;
import halo.corebridge.notification.model.dto.NotificationDto;
import halo.corebridge.notification.model.entity.Notification;
import halo.corebridge.notification.repository.NotificationRepository;
import halo.corebridge.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationService {

    private final Snowflake snowflake = new Snowflake();
    private final NotificationRepository notificationRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 알림 생성 (내부 서비스에서 호출)
     * DB 저장 후 Redis Pub/Sub로 SSE 푸시 발행
     */
    @Transactional
    public NotificationDto.CreateResponse create(NotificationDto.CreateRequest request) {
        try {
            Notification notification = request.toEntity(snowflake.nextId());
            Notification saved = notificationRepository.save(notification);

            log.info("알림 생성 완료: userId={}, type={}, id={}",
                    request.getUserId(), request.getType(), saved.getId());

            // Redis Pub/Sub로 실시간 푸시
            publishToRedis(saved);

            return NotificationDto.CreateResponse.success(saved.getId());
        } catch (Exception e) {
            log.error("알림 생성 실패: userId={}, type={}, error={}",
                    request.getUserId(), request.getType(), e.getMessage());
            return NotificationDto.CreateResponse.fail(e.getMessage());
        }
    }

    /**
     * Redis Pub/Sub로 알림 이벤트 발행
     * - 모든 notification 인스턴스가 수신 → 해당 유저의 SSE 연결을 가진 인스턴스가 전송
     */
    private void publishToRedis(Notification notification) {
        try {
            NotificationDto.Response dto = NotificationDto.Response.from(notification);
            Map<String, Object> message = Map.of(
                    "userId", notification.getUserId(),
                    "eventName", "notification",
                    "data", dto
            );
            String json = objectMapper.writeValueAsString(message);
            stringRedisTemplate.convertAndSend(RedisConfig.NOTIFICATION_CHANNEL, json);
            log.debug("[Pub/Sub] 알림 발행: userId={}, id={}", notification.getUserId(), notification.getId());
        } catch (Exception e) {
            // Redis 발행 실패해도 DB 저장은 이미 완료 → 로그만 남김
            log.warn("[Pub/Sub] 알림 발행 실패 (DB 저장은 완료): userId={}, error={}",
                    notification.getUserId(), e.getMessage());
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
     * 최근 알림 10개 조회
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
