package halo.corebridge.apply.model.dto;

import halo.corebridge.apply.model.entity.Apply;
import halo.corebridge.apply.model.entity.RecruitmentProcess;
import halo.corebridge.apply.model.enums.ProcessStep;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class ApplyDto {

    // ============================================
    // Request
    // ============================================

    @Getter
    @Builder
    public static class CreateRequest {
        private Long jobpostingId;
        private Long userId;
        private Long resumeId;
        private String coverLetter;
    }

    @Getter
    @Builder
    public static class UpdateMemoRequest {
        private String memo;
    }

    // ============================================
    // Response
    // ============================================

    /**
     * 지원 정보 응답 (기본)
     */
    @Getter
    @Builder
    public static class ApplyResponse {
        private Long applyId;
        private Long jobpostingId;
        private Long userId;
        private Long resumeId;
        private String coverLetter;
        private String memo;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static ApplyResponse from(Apply apply) {
            return ApplyResponse.builder()
                    .applyId(apply.getApplyId())
                    .jobpostingId(apply.getJobpostingId())
                    .userId(apply.getUserId())
                    .resumeId(apply.getResumeId())
                    .coverLetter(apply.getCoverLetter())
                    .memo(apply.getMemo())
                    .createdAt(apply.getCreatedAt())
                    .updatedAt(apply.getUpdatedAt())
                    .build();
        }
    }

    /**
     * 지원 상세 응답 (프로세스 정보 포함)
     */
    @Getter
    @Builder
    public static class ApplyDetailResponse {
        // Apply 정보
        private Long applyId;
        private Long jobpostingId;
        private Long userId;
        private Long resumeId;
        private String coverLetter;
        private String memo;
        private LocalDateTime appliedAt;
        
        // Process 정보 (State Machine)
        private Long processId;
        private ProcessStep currentStep;
        private String currentStepName;
        private ProcessStep previousStep;
        private String previousStepName;
        private Set<String> allowedNextSteps;
        private boolean completed;
        private boolean passed;
        private boolean failed;
        private LocalDateTime stepChangedAt;

        public static ApplyDetailResponse from(Apply apply, RecruitmentProcess process) {
            return ApplyDetailResponse.builder()
                    .applyId(apply.getApplyId())
                    .jobpostingId(apply.getJobpostingId())
                    .userId(apply.getUserId())
                    .resumeId(apply.getResumeId())
                    .coverLetter(apply.getCoverLetter())
                    .memo(apply.getMemo())
                    .appliedAt(apply.getCreatedAt())
                    // Process 정보
                    .processId(process.getProcessId())
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
                    .build();
        }
    }

    /**
     * 지원 목록 응답
     */
    @Getter
    @Builder
    public static class ApplyPageResponse {
        private List<ApplyDetailResponse> applies;
        private Long totalCount;

        public static ApplyPageResponse of(List<ApplyDetailResponse> applies, Long count) {
            return ApplyPageResponse.builder()
                    .applies(applies)
                    .totalCount(count)
                    .build();
        }
    }
}
