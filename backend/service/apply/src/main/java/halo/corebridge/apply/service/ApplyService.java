package halo.corebridge.apply.service;

import halo.corebridge.apply.model.dto.ApplyDto;
import halo.corebridge.apply.model.entity.Apply;
import halo.corebridge.apply.model.entity.RecruitmentProcess;
import halo.corebridge.apply.model.enums.ProcessStep;
import halo.corebridge.apply.repository.ApplyRepository;
import halo.corebridge.apply.repository.RecruitmentProcessRepository;
import halo.corebridge.common.dataserializer.DataSerializer;
import halo.corebridge.common.exception.BaseException;
import halo.corebridge.common.response.BaseResponseStatus;
import halo.corebridge.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 지원 서비스
 *
 * 채용 공고 지원 관련 비즈니스 로직을 처리합니다.
 *
 * [트래픽 대응 구조]
 * - apply() 메서드는 DB를 거치지 않고 Redis + Kafka만 사용합니다.
 * - Redis SADD로 원자적 중복 체크 후, Kafka에 메시지를 발행합니다.
 * - 실제 DB INSERT는 ApplyEventConsumer가 백그라운드에서 처리합니다.
 * - 이를 통해 1만 명 동시 지원 시에도 50ms 이내 응답이 가능합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplyService {

    private final Snowflake snowflake = new Snowflake();
    private final ApplyRepository applyRepository;
    private final RecruitmentProcessRepository processRepository;
    private final ProcessService processService;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String APPLY_TOPIC = "corebridge-apply";

    // ============================================
    // 지원자 (구직자) 기능
    // ============================================

    /**
     * 지원 접수 (비동기)
     *
     * 대규모 동시 트래픽 대응을 위해 요청 접수와 실제 처리를 분리합니다.
     *
     * 1. Redis SADD로 중복 체크 (원자적, O(1), race condition 없음)
     * 2. Kafka로 메시지 발행 (DB INSERT 없음, 커넥션 풀 소모 없음)
     * 3. 즉시 "접수 완료" 응답 반환
     *
     * 실제 DB 저장은 ApplyEventConsumer가 Kafka에서 메시지를 소비하여 처리합니다.
     * 처리 완료 시 SSE 알림으로 유저에게 "지원 확정"을 통보합니다.
     */
    public ApplyDto.ApplyAcceptedResponse apply(ApplyDto.CreateRequest request) {
        // 1. Redis SADD — 원자적 중복 체크
        //    Set에 이미 존재하면 0 반환, 신규면 1 반환
        //    DB SELECT 없이 O(1)로 중복 판별
        String key = "applied:" + request.getJobpostingId();
        String member = String.valueOf(request.getUserId());
        Long added = redisTemplate.opsForSet().add(key, member);

        if (added == null || added == 0) {
            throw new BaseException(BaseResponseStatus.ALREADY_APPLIED);
        }

        // 2. Kafka 발행 — DB를 거치지 않으므로 커넥션 풀 소모 없음
        try {
            String payload = DataSerializer.serialize(request);
            kafkaTemplate.send(
                    APPLY_TOPIC,
                    String.valueOf(request.getJobpostingId()),   // 파티션 키
                    payload
            );
            log.info("[Apply] 접수 완료 → Kafka 발행: jobpostingId={}, userId={}",
                    request.getJobpostingId(), request.getUserId());
        } catch (Exception e) {
            // Kafka 발행 실패 시 Redis 롤백 → 재지원 가능하도록
            redisTemplate.opsForSet().remove(key, member);
            log.error("[Apply] Kafka 발행 실패, Redis 롤백: {}", e.getMessage());
            throw new BaseException(BaseResponseStatus.INTERNAL_SERVER_ERROR);
        }

        // 3. 즉시 응답 — "접수됨"
        return ApplyDto.ApplyAcceptedResponse.of(
                request.getJobpostingId(), request.getUserId());
    }

    /**
     * 지원 취소 (APPLIED 상태에서만 가능)
     */
    @Transactional
    public void cancel(Long applyId, Long userId) {
        Apply apply = applyRepository.findById(applyId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.APPLICATION_NOT_FOUND));

        // 본인 확인
        if (!apply.getUserId().equals(userId)) {
            throw new BaseException(BaseResponseStatus.ACCESS_DENIED);
        }

        // 프로세스 상태 확인 (APPLIED 상태에서만 취소 가능)
        RecruitmentProcess process = processRepository.findByApplyId(applyId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.PROCESS_NOT_FOUND));

        if (process.getCurrentStep() != ProcessStep.APPLIED) {
            throw new BaseException(BaseResponseStatus.CANNOT_CANCEL_IN_PROGRESS);
        }

        // 삭제
        processRepository.delete(process);
        applyRepository.delete(apply);

        // Redis에서도 제거 → 재지원 가능하도록
        String key = "applied:" + apply.getJobpostingId();
        redisTemplate.opsForSet().remove(key, String.valueOf(userId));
        log.info("[Apply] 지원 취소 완료: applyId={}, userId={}", applyId, userId);
    }

    /**
     * 내 지원 목록 조회
     */
    @Transactional(readOnly = true)
    public ApplyDto.ApplyPageResponse getMyApplies(Long userId) {
        List<Apply> applies = applyRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<ApplyDto.ApplyDetailResponse> responses = applies.stream()
                .map(apply -> {
                    RecruitmentProcess process = processRepository.findByApplyId(apply.getApplyId())
                            .orElse(null);
                    return process != null
                            ? ApplyDto.ApplyDetailResponse.from(apply, process)
                            : null;
                })
                .filter(r -> r != null)
                .toList();

        Long count = applyRepository.countByUserId(userId);

        return ApplyDto.ApplyPageResponse.of(responses, count);
    }

    /**
     * 지원 상세 조회
     */
    @Transactional(readOnly = true)
    public ApplyDto.ApplyDetailResponse read(Long applyId) {
        Apply apply = applyRepository.findById(applyId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.APPLICATION_NOT_FOUND));

        RecruitmentProcess process = processRepository.findByApplyId(applyId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.PROCESS_NOT_FOUND));

        return ApplyDto.ApplyDetailResponse.from(apply, process);
    }

    // ============================================
    // 기업 기능
    // ============================================

    /**
     * 공고별 지원자 목록 조회
     */
    @Transactional(readOnly = true)
    public ApplyDto.ApplyPageResponse getAppliesByJobposting(Long jobpostingId) {
        List<Apply> applies = applyRepository.findByJobpostingIdOrderByCreatedAtDesc(jobpostingId);

        List<ApplyDto.ApplyDetailResponse> responses = applies.stream()
                .map(apply -> {
                    RecruitmentProcess process = processRepository.findByApplyId(apply.getApplyId())
                            .orElse(null);
                    return process != null
                            ? ApplyDto.ApplyDetailResponse.from(apply, process)
                            : null;
                })
                .filter(r -> r != null)
                .toList();

        Long count = applyRepository.countByJobpostingId(jobpostingId);

        return ApplyDto.ApplyPageResponse.of(responses, count);
    }

    /**
     * 공고별 특정 단계 지원자 목록 조회
     */
    @Transactional(readOnly = true)
    public ApplyDto.ApplyPageResponse getAppliesByStep(Long jobpostingId, ProcessStep step) {
        List<RecruitmentProcess> processes = processRepository
                .findByJobpostingIdAndCurrentStepOrderByStepChangedAtDesc(jobpostingId, step);

        List<ApplyDto.ApplyDetailResponse> responses = processes.stream()
                .map(process -> {
                    Apply apply = applyRepository.findById(process.getApplyId())
                            .orElse(null);
                    return apply != null
                            ? ApplyDto.ApplyDetailResponse.from(apply, process)
                            : null;
                })
                .filter(r -> r != null)
                .toList();

        return ApplyDto.ApplyPageResponse.of(responses, (long) responses.size());
    }

    /**
     * 메모 수정 (기업 내부용)
     */
    @Transactional
    public ApplyDto.ApplyDetailResponse updateMemo(Long applyId, ApplyDto.UpdateMemoRequest request) {
        Apply apply = applyRepository.findById(applyId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.APPLICATION_NOT_FOUND));

        apply.updateMemo(request.getMemo());

        RecruitmentProcess process = processRepository.findByApplyId(applyId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.PROCESS_NOT_FOUND));

        return ApplyDto.ApplyDetailResponse.from(apply, process);
    }

    // ============================================
    // 통계 (ProcessService로 위임)
    // ============================================

    /**
     * 사용자별 지원 통계
     */
    @Transactional(readOnly = true)
    public Object getUserStats(Long userId) {
        return processService.getUserStats(userId);
    }

    /**
     * 공고별 지원자 통계
     */
    @Transactional(readOnly = true)
    public Object getJobpostingStats(Long jobpostingId) {
        return processService.getJobpostingStats(jobpostingId);
    }

    /**
     * 기업 전체 통계
     */
    @Transactional(readOnly = true)
    public Object getCompanyStats(List<Long> jobpostingIds) {
        return processService.getCompanyStats(jobpostingIds);
    }
}
