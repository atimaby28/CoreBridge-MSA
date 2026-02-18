package halo.corebridge.common.event.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Outbox 이벤트 ID 기반 멱등성 체크.
 * Consumer 측에서 동일 이벤트 중복 처리를 방지합니다.
 * ProcessedEventRepository Bean이 존재하는 서비스에서만 활성화됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(ProcessedEventRepository.class)
public class IdempotencyChecker {

    private final ProcessedEventRepository processedEventRepository;

    /**
     * 이벤트가 이미 처리되었는지 확인합니다.
     * @param eventId Outbox 이벤트 UUID
     * @param consumerGroup Consumer 그룹명
     * @return true: 이미 처리됨 (스킵 필요), false: 미처리 (처리 진행)
     */
    public boolean isDuplicate(String eventId, String consumerGroup) {
        if (eventId == null || eventId.isBlank()) {
            return false; // eventId 없는 레거시 이벤트는 중복 체크 스킵
        }
        return processedEventRepository.existsByEventIdAndConsumerGroup(eventId, consumerGroup);
    }

    /**
     * 이벤트 처리 완료를 기록합니다.
     * @param eventId Outbox 이벤트 UUID
     * @param consumerGroup Consumer 그룹명
     */
    public void markAsProcessed(String eventId, String consumerGroup) {
        if (eventId == null || eventId.isBlank()) {
            return;
        }
        try {
            processedEventRepository.save(ProcessedEvent.create(eventId, consumerGroup));
        } catch (DataIntegrityViolationException e) {
            log.warn("[IdempotencyChecker] duplicate eventId={}, consumerGroup={} (concurrent processing)", eventId, consumerGroup);
        }
    }
}
