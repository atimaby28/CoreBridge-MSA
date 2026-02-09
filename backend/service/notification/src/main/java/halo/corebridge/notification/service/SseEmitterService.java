package halo.corebridge.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 연결 관리.
 * userId → SseEmitter 매핑을 유지하고, 이벤트 전송/정리를 담당.
 */
@Slf4j
@Service
public class SseEmitterService {

    private static final long SSE_TIMEOUT = 60 * 60 * 1000L; // 1시간

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * SSE 연결 생성 및 등록
     */
    public SseEmitter subscribe(Long userId) {
        // 기존 연결이 있으면 정리
        SseEmitter existing = emitters.get(userId);
        if (existing != null) {
            existing.complete();
            emitters.remove(userId);
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> {
            log.debug("[SSE] 연결 종료: userId={}", userId);
            emitters.remove(userId);
        });
        emitter.onTimeout(() -> {
            log.debug("[SSE] 타임아웃: userId={}", userId);
            emitter.complete();
            emitters.remove(userId);
        });
        emitter.onError(e -> {
            log.debug("[SSE] 에러: userId={}, error={}", userId, e.getMessage());
            emitters.remove(userId);
        });

        emitters.put(userId, emitter);
        log.info("[SSE] 연결 등록: userId={}, 총 연결 수={}", userId, emitters.size());

        // 연결 직후 더미 이벤트 전송 (브라우저 연결 확인용)
        sendToUser(userId, "connect", "connected");

        return emitter;
    }

    /**
     * 특정 사용자에게 SSE 이벤트 전송
     */
    public void sendToUser(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
            log.debug("[SSE] 이벤트 전송 성공: userId={}, event={}", userId, eventName);
        } catch (IOException e) {
            log.warn("[SSE] 전송 실패 (연결 제거): userId={}, error={}", userId, e.getMessage());
            emitters.remove(userId);
        }
    }

    /**
     * Heartbeat — 15초마다 모든 SSE 연결에 빈 코멘트 전송.
     * 프록시(Vite, Nginx 등)가 idle 연결을 끊지 않도록 keep-alive 역할.
     */
    @Scheduled(fixedRate = 15000)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) return;

        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException e) {
                log.debug("[SSE] heartbeat 실패 (연결 제거): userId={}", userId);
                emitters.remove(userId);
            }
        });
    }

    /**
     * 현재 SSE 연결 수
     */
    public int getConnectionCount() {
        return emitters.size();
    }

    /**
     * 특정 사용자의 SSE 연결 여부
     */
    public boolean isConnected(Long userId) {
        return emitters.containsKey(userId);
    }
}
