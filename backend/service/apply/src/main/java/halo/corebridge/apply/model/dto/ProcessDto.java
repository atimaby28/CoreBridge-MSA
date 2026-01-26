package halo.corebridge.apply.model.dto;

import halo.corebridge.apply.model.entity.ProcessHistory;
import halo.corebridge.apply.model.entity.RecruitmentProcess;
import halo.corebridge.apply.model.enums.ProcessStep;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class ProcessDto {

    // ============================================
    // Request
    // ============================================

    @Getter
    @Builder
    public static class CreateRequest {
        private Long applyId;
        private Long jobpostingId;
        private Long userId;
    }

    @Getter
    @Builder
    public static class TransitionRequest {
        private ProcessStep nextStep;
        private Long changedBy;
        private String reason;
        private String note;
    }

    // ============================================
    // Response
    // ============================================

    @Getter
    @Builder
    public static class ProcessResponse {
        private Long processId;
        private Long applyId;
        private Long jobpostingId;
        private Long userId;
        private ProcessStep currentStep;
        private String currentStepName;
        private ProcessStep previousStep;
        private String previousStepName;
        private Set<String> allowedNextSteps;
        private boolean completed;
        private boolean passed;
        private boolean failed;
        private LocalDateTime stepChangedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static ProcessResponse from(RecruitmentProcess process) {
            return ProcessResponse.builder()
                    .processId(process.getProcessId())
                    .applyId(process.getApplyId())
                    .jobpostingId(process.getJobpostingId())
                    .userId(process.getUserId())
                    .currentStep(process.getCurrentStep())
                    .currentStepName(process.getCurrentStep().getDisplayName())
                    .previousStep(process.getPreviousStep())
                    .previousStepName(process.getPreviousStep() != null
                            ? process.getPreviousStep().getDisplayName() : null)
                    .allowedNextSteps(process.getCurrentStep().getAllowedNextSteps())
                    .completed(process.isCompleted())
                    .passed(process.isPassed())
                    .failed(process.isFailed())
                    .stepChangedAt(process.getStepChangedAt())
                    .createdAt(process.getCreatedAt())
                    .updatedAt(process.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ProcessPageResponse {
        private List<ProcessResponse> processes;
        private Long processCount;

        public static ProcessPageResponse of(List<ProcessResponse> processes, Long count) {
            return ProcessPageResponse.builder()
                    .processes(processes)
                    .processCount(count)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class HistoryResponse {
        private Long historyId;
        private Long processId;
        private Long applyId;
        private ProcessStep fromStep;
        private String fromStepName;
        private ProcessStep toStep;
        private String toStepName;
        private Long changedBy;
        private String reason;
        private String note;
        private LocalDateTime createdAt;

        public static HistoryResponse from(ProcessHistory history) {
            return HistoryResponse.builder()
                    .historyId(history.getHistoryId())
                    .processId(history.getProcessId())
                    .applyId(history.getApplyId())
                    .fromStep(history.getFromStep())
                    .fromStepName(history.getFromStep() != null
                            ? history.getFromStep().getDisplayName() : null)
                    .toStep(history.getToStep())
                    .toStepName(history.getToStep().getDisplayName())
                    .changedBy(history.getChangedBy())
                    .reason(history.getReason())
                    .note(history.getNote())
                    .createdAt(history.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class StepInfoResponse {
        private ProcessStep step;
        private String displayName;
        private Set<String> allowedNextSteps;
        private boolean terminal;

        public static StepInfoResponse from(ProcessStep step) {
            return StepInfoResponse.builder()
                    .step(step)
                    .displayName(step.getDisplayName())
                    .allowedNextSteps(step.getAllowedNextSteps())
                    .terminal(step.isTerminal())
                    .build();
        }
    }

    // ============================================
    // 통계 Response
    // ============================================

    @Getter
    @Builder
    public static class UserStatsResponse {
        private Long totalProcesses;       // 총 지원 수
        private Long pendingProcesses;     // 진행 중
        private Long passedProcesses;      // 합격 (FINAL_PASS)
        private Long failedProcesses;      // 탈락 (모든 FAIL 상태)
        private Double passRate;           // 합격률

        public static UserStatsResponse of(Long total, Long pending, Long passed, Long failed) {
            double rate = 0.0;
            if (passed + failed > 0) {
                rate = Math.round((double) passed / (passed + failed) * 100 * 10) / 10.0;
            }
            return UserStatsResponse.builder()
                    .totalProcesses(total)
                    .pendingProcesses(pending)
                    .passedProcesses(passed)
                    .failedProcesses(failed)
                    .passRate(rate)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class CompanyStatsResponse {
        private Long totalApplicants;         // 총 지원자 수
        private Long pendingApplicants;       // 검토 대기 중
        private Long interviewingApplicants;  // 면접 진행 중
        private Long passedApplicants;        // 합격자 수
        private Long failedApplicants;        // 탈락자 수
        private Double passRate;              // 합격률

        public static CompanyStatsResponse of(Long total, Long pending, Long interviewing, Long passed, Long failed) {
            double rate = 0.0;
            if (passed + failed > 0) {
                rate = Math.round((double) passed / (passed + failed) * 100 * 10) / 10.0;
            }
            return CompanyStatsResponse.builder()
                    .totalApplicants(total)
                    .pendingApplicants(pending)
                    .interviewingApplicants(interviewing)
                    .passedApplicants(passed)
                    .failedApplicants(failed)
                    .passRate(rate)
                    .build();
        }
    }
}
