package halo.corebridge.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub 메시지 수신 → SSE로 전달.
 *
 * 메시지 형식 (JSON):
 * {
 *   "userId": 123,
 *   "eventName": "notification",
 *   "data": { ... notification dto ... }
 * }
 *
 * 다중 인스턴스 대응:
 * - notification 서비스가 여러 인스턴스일 때, 어느 인스턴스에 SSE 연결이 있는지 모름
 * - Redis Pub/Sub로 전체 인스턴스에 브로드캐스트 → 해당 유저의 연결을 가진 인스턴스만 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final SseEmitterService sseEmitterService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            JsonNode json = objectMapper.readTree(body);

            Long userId = json.get("userId").asLong();
            String eventName = json.has("eventName") ? json.get("eventName").asText() : "notification";
            String data = json.has("data") ? json.get("data").toString() : body;

            // 이 인스턴스에 해당 유저의 SSE 연결이 있을 때만 전송
            if (sseEmitterService.isConnected(userId)) {
                sseEmitterService.sendToUser(userId, eventName, data);
                log.debug("[RedisSubscriber] SSE 전송: userId={}, event={}", userId, eventName);
            }
        } catch (Exception e) {
            log.error("[RedisSubscriber] 메시지 처리 실패: {}", e.getMessage(), e);
        }
    }
}
