package halo.corebridge.apply.consumer;

import halo.corebridge.apply.client.NotificationClient;
import halo.corebridge.apply.model.dto.ApplyDto;
import halo.corebridge.apply.model.entity.Apply;
import halo.corebridge.apply.model.entity.RecruitmentProcess;
import halo.corebridge.apply.model.enums.ProcessStep;
import halo.corebridge.apply.repository.ApplyRepository;
import halo.corebridge.apply.service.ProcessService;
import halo.corebridge.common.dataserializer.DataSerializer;
import halo.corebridge.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지원 이벤트 Consumer
 *
 * Kafka에서 지원 요청 메시지를 소비하여 실제 DB 저장을 수행합니다.
 * concurrency=3으로 3개 스레드가 파티션별 병렬 처리합니다.
 *
 * [처리 흐름]
 * 1. Kafka에서 메시지 소비
 * 2. DB 레벨 중복 방어 (Redis 장애 대비 안전장치)
 * 3. Apply + RecruitmentProcess DB INSERT
 * 4. SSE 알림 전송 → 유저에게 "지원 확정" 통보
 *
 * 1만 건이 Kafka에 쌓여도 DB 커넥션 풀(3개) 범위 내에서
 * 안정적으로 순차 처리됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplyEventConsumer {

    private final Snowflake snowflake = new Snowflake();
    private final ApplyRepository applyRepository;
    private final ProcessService processService;
    private final NotificationClient notificationClient;
    private final StringRedisTemplate redisTemplate;

    @KafkaListener(
            topics = "corebridge-apply",
            groupId = "apply-consumer-group",
            concurrency = "3"
    )
    @Transactional
    public void consume(String message) {
        ApplyDto.CreateRequest request = DataSerializer.deserialize(
                message, ApplyDto.CreateRequest.class);

        if (request == null) {
            log.error("[ApplyConsumer] 메시지 역직렬화 실패");
            return;
        }

        try {
            // DB 레벨 중복 방어 (Redis 장애 시 안전장치)
            // 정상 상황에서는 Redis SADD에서 이미 걸러지므로 여기까지 올 일 없음
            if (applyRepository.existsByJobpostingIdAndUserId(
                    request.getJobpostingId(), request.getUserId())) {
                log.warn("[ApplyConsumer] DB 중복 감지 (스킵): jobpostingId={}, userId={}",
                        request.getJobpostingId(), request.getUserId());
                return;
            }

            // Apply 생성
            Apply apply = Apply.create(
                    snowflake.nextId(),
                    request.getJobpostingId(),
                    request.getUserId(),
                    request.getResumeId(),
                    request.getCoverLetter()
            );
            applyRepository.save(apply);

            // RecruitmentProcess 생성 (State Machine 시작)
            RecruitmentProcess process = processService.createProcess(
                    apply.getApplyId(),
                    apply.getJobpostingId(),
                    apply.getUserId()
            );

            log.info("[ApplyConsumer] 지원 처리 완료: applyId={}, jobpostingId={}, userId={}",
                    apply.getApplyId(), request.getJobpostingId(), request.getUserId());

            // SSE 알림 — "지원이 확정되었습니다"
            // NotificationClient.sendProcessNotification은 @Async이므로 블로킹 없음
            notificationClient.sendProcessNotification(
                    request.getUserId(),
                    ProcessStep.APPLIED,
                    apply.getApplyId(),
                    request.getJobpostingId()
            );

        } catch (Exception e) {
            // 처리 실패 시 Redis에서 제거 → 유저가 재지원할 수 있도록
            String key = "applied:" + request.getJobpostingId();
            redisTemplate.opsForSet().remove(key, String.valueOf(request.getUserId()));
            log.error("[ApplyConsumer] 처리 실패 (Redis 롤백): jobpostingId={}, userId={}, error={}",
                    request.getJobpostingId(), request.getUserId(), e.getMessage(), e);
        }
    }
}
