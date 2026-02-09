package halo.corebridge.apply.service;

import halo.corebridge.apply.client.NotificationClient;
import halo.corebridge.apply.model.dto.ProcessDto;
import halo.corebridge.apply.model.entity.ProcessHistory;
import halo.corebridge.apply.model.entity.RecruitmentProcess;
import halo.corebridge.apply.model.enums.ProcessStep;
import halo.corebridge.apply.repository.ProcessHistoryRepository;
import halo.corebridge.apply.repository.RecruitmentProcessRepository;
import halo.corebridge.common.event.EventType;
import halo.corebridge.common.event.NotificationCreatedEventPayload;
import halo.corebridge.common.exception.BaseException;
import halo.corebridge.common.outboxmessagerelay.OutboxEventPublisher;
import halo.corebridge.common.response.BaseResponseStatus;
import halo.corebridge.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 채용 프로세스 서비스 (State Machine)
 *
 * 지원자의 채용 프로세스 상태를 관리합니다.
 * State Machine 패턴을 적용하여 허용된 상태 전이만 가능하도록 합니다.
 */
@Service
@RequiredArgsConstructor
public class ProcessService {

    private final Snowflake snowflake = new Snowflake();
    private final RecruitmentProcessRepository processRepository;
    private final ProcessHistoryRepository historyRepository;
    private final NotificationClient notificationClient;
    private final OutboxEventPublisher outboxEventPublisher;

    // ============================================
    // 프로세스 생성 (ApplyService에서 내부 호출)
    // ============================================

    /**
     * 채용 프로세스 생성 (지원 시 함께 생성)
     */
    @Transactional
    public RecruitmentProcess createProcess(Long applyId, Long jobpostingId, Long userId) {
        RecruitmentProcess process = RecruitmentProcess.create(
                snowflake.nextId(),
                applyId,
                jobpostingId,
                userId
        );

        processRepository.save(process);

        // 최초 이력 저장
        ProcessHistory history = ProcessHistory.create(
                snowflake.nextId(),
                process.getProcessId(),
                applyId,
                null,
                ProcessStep.APPLIED,
                null,
                "지원 완료",
                null
        );
        historyRepository.save(history);

        return process;
    }

    // ============================================
    // 상태 전이 (State Machine 핵심)
    // ============================================

    /**
     * 상태 전이 (규칙 검증 후 전이)
     *
     * State Machine의 핵심 메서드입니다.
     * ProcessStep enum에 정의된 규칙에 따라 허용된 전이만 수행합니다.
     */
    @Transactional
    public ProcessDto.ProcessResponse transition(Long processId, ProcessDto.TransitionRequest request) {
        RecruitmentProcess process = processRepository.findById(processId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.PROCESS_NOT_FOUND));

        ProcessStep fromStep = process.getCurrentStep();
        ProcessStep toStep = request.getNextStep();

        // State Machine: 전이 규칙 검증 (내부에서 예외 발생)
        process.transition(toStep);

        // 이력 저장
        ProcessHistory history = ProcessHistory.create(
                snowflake.nextId(),
                processId,
                process.getApplyId(),
                fromStep,
                toStep,
                request.getChangedBy(),
                request.getReason(),
                request.getNote()
        );
        historyRepository.save(history);

        // 알림 이벤트 발행 (Outbox → Kafka → notification 서비스)
        publishNotificationEvent(process, toStep);

        return ProcessDto.ProcessResponse.from(process);
    }

    /**
     * 지원 ID로 상태 전이
     */
    @Transactional
    public ProcessDto.ProcessResponse transitionByApplyId(Long applyId, ProcessDto.TransitionRequest request) {
        RecruitmentProcess process = processRepository.findByApplyId(applyId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.PROCESS_NOT_FOUND));

        return transition(process.getProcessId(), request);
    }

    // ============================================
    // 조회
    // ============================================

    /**
     * 프로세스 상세 조회
     */
    @Transactional(readOnly = true)
    public ProcessDto.ProcessResponse read(Long processId) {
        RecruitmentProcess process = processRepository.findById(processId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.PROCESS_NOT_FOUND));

        return ProcessDto.ProcessResponse.from(process);
    }

    /**
     * 지원 ID로 프로세스 조회
     */
    @Transactional(readOnly = true)
    public ProcessDto.ProcessResponse readByApplyId(Long applyId) {
        RecruitmentProcess process = processRepository.findByApplyId(applyId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.PROCESS_NOT_FOUND));

        return ProcessDto.ProcessResponse.from(process);
    }

    /**
     * 공고별 프로세스 목록
     */
    @Transactional(readOnly = true)
    public ProcessDto.ProcessPageResponse getByJobposting(Long jobpostingId) {
        List<ProcessDto.ProcessResponse> processes = processRepository
                .findByJobpostingIdOrderByCreatedAtDesc(jobpostingId)
                .stream()
                .map(ProcessDto.ProcessResponse::from)
                .toList();

        Long count = processRepository.countByJobpostingId(jobpostingId);

        return ProcessDto.ProcessPageResponse.of(processes, count);
    }

    /**
     * 공고별 특정 단계 프로세스 목록
     */
    @Transactional(readOnly = true)
    public ProcessDto.ProcessPageResponse getByJobpostingAndStep(Long jobpostingId, ProcessStep step) {
        List<ProcessDto.ProcessResponse> processes = processRepository
                .findByJobpostingIdAndCurrentStepOrderByStepChangedAtDesc(jobpostingId, step)
                .stream()
                .map(ProcessDto.ProcessResponse::from)
                .toList();

        return ProcessDto.ProcessPageResponse.of(processes, (long) processes.size());
    }

    /**
     * 사용자별 프로세스 목록 (내 지원 현황)
     */
    @Transactional(readOnly = true)
    public ProcessDto.ProcessPageResponse getByUser(Long userId) {
        List<ProcessDto.ProcessResponse> processes = processRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ProcessDto.ProcessResponse::from)
                .toList();

        Long count = processRepository.countByUserId(userId);

        return ProcessDto.ProcessPageResponse.of(processes, count);
    }

    /**
     * 상태 변경 이력 조회
     */
    @Transactional(readOnly = true)
    public List<ProcessDto.HistoryResponse> getHistory(Long processId) {
        return historyRepository.findByProcessIdOrderByCreatedAtDesc(processId)
                .stream()
                .map(ProcessDto.HistoryResponse::from)
                .toList();
    }

    /**
     * 지원 ID로 상태 변경 이력 조회
     */
    @Transactional(readOnly = true)
    public List<ProcessDto.HistoryResponse> getHistoryByApplyId(Long applyId) {
        return historyRepository.findByApplyIdOrderByCreatedAtDesc(applyId)
                .stream()
                .map(ProcessDto.HistoryResponse::from)
                .toList();
    }

    // ============================================
    // 메타 정보
    // ============================================

    /**
     * 모든 단계 정보 조회 (프론트엔드용)
     */
    public List<ProcessDto.StepInfoResponse> getAllSteps() {
        return Arrays.stream(ProcessStep.values())
                .map(ProcessDto.StepInfoResponse::from)
                .toList();
    }

    // ============================================
    // 통계 API
    // ============================================

    /**
     * 사용자 통계 (구직자용 Dashboard)
     */
    @Transactional(readOnly = true)
    public ProcessDto.UserStatsResponse getUserStats(Long userId) {
        Long total = processRepository.countByUserId(userId);

        // 진행 중: 모든 비종료 상태
        List<ProcessStep> pendingSteps = List.of(
                ProcessStep.APPLIED,
                ProcessStep.DOCUMENT_REVIEW,
                ProcessStep.DOCUMENT_PASS,
                ProcessStep.CODING_TEST,
                ProcessStep.CODING_PASS,
                ProcessStep.INTERVIEW_1,
                ProcessStep.INTERVIEW_1_PASS,
                ProcessStep.INTERVIEW_2,
                ProcessStep.INTERVIEW_2_PASS,
                ProcessStep.FINAL_REVIEW
        );
        Long pending = processRepository.countByUserIdAndCurrentStepIn(userId, pendingSteps);

        // 합격: FINAL_PASS
        Long passed = processRepository.countByUserIdAndCurrentStep(userId, ProcessStep.FINAL_PASS);

        // 탈락: 모든 FAIL 상태
        List<ProcessStep> failSteps = List.of(
                ProcessStep.DOCUMENT_FAIL,
                ProcessStep.CODING_FAIL,
                ProcessStep.INTERVIEW_1_FAIL,
                ProcessStep.INTERVIEW_2_FAIL,
                ProcessStep.FINAL_FAIL
        );
        Long failed = processRepository.countByUserIdAndCurrentStepIn(userId, failSteps);

        return ProcessDto.UserStatsResponse.of(total, pending, passed, failed);
    }

    /**
     * 공고별 통계 (기업용)
     */
    @Transactional(readOnly = true)
    public ProcessDto.CompanyStatsResponse getJobpostingStats(Long jobpostingId) {
        Long total = processRepository.countByJobpostingId(jobpostingId);

        // 검토 대기: APPLIED, DOCUMENT_REVIEW
        List<ProcessStep> pendingSteps = List.of(ProcessStep.APPLIED, ProcessStep.DOCUMENT_REVIEW);
        Long pending = processRepository.countByJobpostingIdAndCurrentStepIn(jobpostingId, pendingSteps);

        // 면접 진행 중
        List<ProcessStep> interviewingSteps = List.of(
                ProcessStep.DOCUMENT_PASS,
                ProcessStep.CODING_TEST,
                ProcessStep.CODING_PASS,
                ProcessStep.INTERVIEW_1,
                ProcessStep.INTERVIEW_1_PASS,
                ProcessStep.INTERVIEW_2,
                ProcessStep.INTERVIEW_2_PASS,
                ProcessStep.FINAL_REVIEW
        );
        Long interviewing = processRepository.countByJobpostingIdAndCurrentStepIn(jobpostingId, interviewingSteps);

        // 합격
        Long passed = processRepository.countByJobpostingIdAndCurrentStep(jobpostingId, ProcessStep.FINAL_PASS);

        // 탈락
        List<ProcessStep> failSteps = List.of(
                ProcessStep.DOCUMENT_FAIL,
                ProcessStep.CODING_FAIL,
                ProcessStep.INTERVIEW_1_FAIL,
                ProcessStep.INTERVIEW_2_FAIL,
                ProcessStep.FINAL_FAIL
        );
        Long failed = processRepository.countByJobpostingIdAndCurrentStepIn(jobpostingId, failSteps);

        return ProcessDto.CompanyStatsResponse.of(total, pending, interviewing, passed, failed);
    }

    /**
     * 기업 전체 통계 (여러 공고 합산)
     */
    @Transactional(readOnly = true)
    public ProcessDto.CompanyStatsResponse getCompanyStats(List<Long> jobpostingIds) {
        if (jobpostingIds == null || jobpostingIds.isEmpty()) {
            return ProcessDto.CompanyStatsResponse.of(0L, 0L, 0L, 0L, 0L);
        }

        Long total = processRepository.countByJobpostingIdIn(jobpostingIds);

        // 검토 대기
        List<ProcessStep> pendingSteps = List.of(ProcessStep.APPLIED, ProcessStep.DOCUMENT_REVIEW);
        Long pending = processRepository.countByJobpostingIdInAndCurrentStepIn(jobpostingIds, pendingSteps);

        // 면접 진행 중
        List<ProcessStep> interviewingSteps = List.of(
                ProcessStep.DOCUMENT_PASS,
                ProcessStep.CODING_TEST,
                ProcessStep.CODING_PASS,
                ProcessStep.INTERVIEW_1,
                ProcessStep.INTERVIEW_1_PASS,
                ProcessStep.INTERVIEW_2,
                ProcessStep.INTERVIEW_2_PASS,
                ProcessStep.FINAL_REVIEW
        );
        Long interviewing = processRepository.countByJobpostingIdInAndCurrentStepIn(jobpostingIds, interviewingSteps);

        // 합격
        List<ProcessStep> passSteps = List.of(ProcessStep.FINAL_PASS);
        Long passed = processRepository.countByJobpostingIdInAndCurrentStepIn(jobpostingIds, passSteps);

        // 탈락
        List<ProcessStep> failSteps = List.of(
                ProcessStep.DOCUMENT_FAIL,
                ProcessStep.CODING_FAIL,
                ProcessStep.INTERVIEW_1_FAIL,
                ProcessStep.INTERVIEW_2_FAIL,
                ProcessStep.FINAL_FAIL
        );
        Long failed = processRepository.countByJobpostingIdInAndCurrentStepIn(jobpostingIds, failSteps);

        return ProcessDto.CompanyStatsResponse.of(total, pending, interviewing, passed, failed);
    }

    // ============================================
    // 알림 이벤트 발행 (Outbox Pattern)
    // ============================================

    /**
     * Outbox → Kafka로 알림 이벤트 발행
     * notification 서비스가 Consumer로 수신하여 DB 저장 + SSE 푸시
     */
    private void publishNotificationEvent(RecruitmentProcess process, ProcessStep toStep) {
        String notificationType = mapStepToNotificationType(toStep);
        if (notificationType == null) {
            return; // 알림 대상이 아닌 상태
        }

        NotificationCreatedEventPayload payload = NotificationCreatedEventPayload.builder()
                .userId(process.getUserId())
                .type(notificationType)
                .title(generateTitle(toStep))
                .message(generateMessage(toStep))
                .link("/my/applications/" + process.getApplyId())
                .relatedId(process.getApplyId())
                .relatedType("APPLY")
                .build();

        outboxEventPublisher.publish(
                EventType.NOTIFICATION_CREATED,
                payload,
                process.getUserId()  // shardKey: userId 기반
        );
    }

    private String mapStepToNotificationType(ProcessStep step) {
        return switch (step) {
            case DOCUMENT_PASS -> "DOCUMENT_PASS";
            case DOCUMENT_FAIL -> "DOCUMENT_FAIL";
            case CODING_TEST -> "CODING_TEST_SCHEDULED";
            case CODING_PASS -> "CODING_TEST_PASS";
            case CODING_FAIL -> "CODING_TEST_FAIL";
            case INTERVIEW_1, INTERVIEW_2 -> "INTERVIEW_SCHEDULED";
            case INTERVIEW_1_PASS, INTERVIEW_2_PASS -> "INTERVIEW_PASS";
            case INTERVIEW_1_FAIL, INTERVIEW_2_FAIL -> "INTERVIEW_FAIL";
            case FINAL_PASS -> "FINAL_PASS";
            case FINAL_FAIL -> "FINAL_FAIL";
            default -> null;
        };
    }

    private String generateTitle(ProcessStep step) {
        return switch (step) {
            case DOCUMENT_PASS -> "서류 전형 합격";
            case DOCUMENT_FAIL -> "서류 전형 결과 안내";
            case CODING_TEST -> "코딩 테스트 안내";
            case CODING_PASS -> "코딩 테스트 합격";
            case CODING_FAIL -> "코딩 테스트 결과 안내";
            case INTERVIEW_1 -> "1차 면접 안내";
            case INTERVIEW_2 -> "2차 면접 안내";
            case INTERVIEW_1_PASS -> "1차 면접 합격";
            case INTERVIEW_2_PASS -> "2차 면접 합격";
            case INTERVIEW_1_FAIL, INTERVIEW_2_FAIL -> "면접 결과 안내";
            case FINAL_PASS -> "🎉 최종 합격을 축하합니다!";
            case FINAL_FAIL -> "최종 결과 안내";
            default -> "채용 진행 상태 변경";
        };
    }

    private String generateMessage(ProcessStep step) {
        return switch (step) {
            case DOCUMENT_PASS -> "서류 전형에 합격하셨습니다. 다음 전형 안내를 확인해주세요.";
            case DOCUMENT_FAIL -> "서류 전형 결과를 확인해주세요.";
            case CODING_TEST -> "코딩 테스트 일정이 안내되었습니다. 상세 내용을 확인해주세요.";
            case CODING_PASS -> "코딩 테스트에 합격하셨습니다. 다음 전형 안내를 확인해주세요.";
            case CODING_FAIL -> "코딩 테스트 결과를 확인해주세요.";
            case INTERVIEW_1 -> "1차 면접 일정이 안내되었습니다. 상세 내용을 확인해주세요.";
            case INTERVIEW_2 -> "2차 면접 일정이 안내되었습니다. 상세 내용을 확인해주세요.";
            case INTERVIEW_1_PASS -> "1차 면접에 합격하셨습니다. 2차 면접 안내를 확인해주세요.";
            case INTERVIEW_2_PASS -> "2차 면접에 합격하셨습니다. 최종 결과를 기다려주세요.";
            case INTERVIEW_1_FAIL, INTERVIEW_2_FAIL -> "면접 결과를 확인해주세요.";
            case FINAL_PASS -> "최종 합격을 진심으로 축하드립니다! 입사 관련 안내를 확인해주세요.";
            case FINAL_FAIL -> "채용 결과를 확인해주세요. 좋은 기회가 있으시길 바랍니다.";
            default -> "채용 진행 상태가 변경되었습니다.";
        };
    }
}
