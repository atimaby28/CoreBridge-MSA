package halo.corebridge.apply.model.entity;

import halo.corebridge.apply.model.enums.ProcessStep;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채용 프로세스 (지원자별 진행 상태)
 * 
 * State Machine 패턴을 적용하여 상태 전이를 관리합니다.
 * Apply와 1:1 관계이며, 지원 시 함께 생성됩니다.
 */
@Entity
@Table(name = "recruitment_process")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentProcess {

    @Id
    private Long processId;

    @Column(nullable = false, unique = true)
    private Long applyId;  // 지원 ID (1:1)

    @Column(nullable = false)
    private Long jobpostingId;   // 채용공고 ID

    @Column(nullable = false)
    private Long userId;         // 지원자 ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessStep currentStep;

    @Enumerated(EnumType.STRING)
    private ProcessStep previousStep;

    private LocalDateTime stepChangedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 프로세스 생성 (팩토리 메서드)
     */
    public static RecruitmentProcess create(
            Long processId,
            Long applyId,
            Long jobpostingId,
            Long userId
    ) {
        RecruitmentProcess process = new RecruitmentProcess();
        process.processId = processId;
        process.applyId = applyId;
        process.jobpostingId = jobpostingId;
        process.userId = userId;
        process.currentStep = ProcessStep.APPLIED;
        process.previousStep = null;
        process.stepChangedAt = LocalDateTime.now();
        process.createdAt = LocalDateTime.now();
        process.updatedAt = process.createdAt;
        return process;
    }

    /**
     * 상태 전이 (State Machine 핵심 메서드)
     * 
     * @param nextStep 전이할 다음 상태
     * @throws IllegalStateException 허용되지 않는 전이인 경우
     */
    public void transition(ProcessStep nextStep) {
        if (!currentStep.canTransitionTo(nextStep)) {
            throw new IllegalStateException(
                    String.format("'%s'에서 '%s'(으)로 전이할 수 없습니다. 허용된 전이: %s", 
                            currentStep.getDisplayName(), 
                            nextStep.getDisplayName(),
                            currentStep.getAllowedNextSteps())
            );
        }

        this.previousStep = this.currentStep;
        this.currentStep = nextStep;
        this.stepChangedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 종료 상태 여부
     */
    public boolean isCompleted() {
        return currentStep.isTerminal();
    }

    /**
     * 합격 여부
     */
    public boolean isPassed() {
        return currentStep.isPass();
    }

    /**
     * 탈락 여부
     */
    public boolean isFailed() {
        return currentStep.isFail();
    }

    /**
     * 진행 중 여부
     */
    public boolean isInProgress() {
        return currentStep.isInProgress();
    }
}
