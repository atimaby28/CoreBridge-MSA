package halo.corebridge.common.event.idempotency;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Outbox 이벤트 ID 기반 멱등성 체크.
 * Consumer 측에서 동일 이벤트 중복 처리를 방지합니다.
 * ProcessedEventRepository가 없는 서비스에서는 멱등성 체크를 스킵합니다.
 */
@Slf4j
@Component
public class IdempotencyChecker {

    private final Optional<ProcessedEventRepository> processedEventRepository;

    public IdempotencyChecker(Optional<ProcessedEventRepository> processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * 이벤트가 이미 처리되었는지 확인합니다.
     * @param eventId Outbox 이벤트 UUID
     * @param consumerGroup Consumer 그룹명
     * @return true: 이미 처리됨 (스킵 필요), false: 미처리 (처리 진행)
     */
    public boolean isDuplicate(String eventId, String consumerGroup) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        return processedEventRepository
                .map(repo -> repo.existsByEventIdAndConsumerGroup(eventId, consumerGroup))
                .orElse(false);
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
        processedEventRepository.ifPresent(repo -> {
            try {
                repo.save(ProcessedEvent.create(eventId, consumerGroup));
            } catch (DataIntegrityViolationException e) {
                log.warn("[IdempotencyChecker] duplicate eventId={}, consumerGroup={} (concurrent processing)", eventId, consumerGroup);
            }
        });
    }
}
