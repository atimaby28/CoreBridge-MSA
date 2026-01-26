package halo.corebridge.apply.model.enums;

import java.util.Set;

/**
 * 채용 프로세스 단계 (State Machine)
 * 
 * 각 상태에서 허용되는 다음 상태를 정의하여
 * 잘못된 상태 전이를 방지합니다.
 */
public enum ProcessStep {
    // 서류 단계
    APPLIED("지원완료", Set.of("DOCUMENT_REVIEW")),
    DOCUMENT_REVIEW("서류검토중", Set.of("DOCUMENT_PASS", "DOCUMENT_FAIL")),
    DOCUMENT_PASS("서류합격", Set.of("CODING_TEST", "INTERVIEW_1")),
    DOCUMENT_FAIL("서류탈락", Set.of()),

    // 코딩테스트
    CODING_TEST("코딩테스트", Set.of("CODING_PASS", "CODING_FAIL")),
    CODING_PASS("코딩테스트합격", Set.of("INTERVIEW_1")),
    CODING_FAIL("코딩테스트탈락", Set.of()),

    // 면접 단계
    INTERVIEW_1("1차면접", Set.of("INTERVIEW_1_PASS", "INTERVIEW_1_FAIL")),
    INTERVIEW_1_PASS("1차면접합격", Set.of("INTERVIEW_2")),
    INTERVIEW_1_FAIL("1차면접탈락", Set.of()),

    INTERVIEW_2("2차면접", Set.of("INTERVIEW_2_PASS", "INTERVIEW_2_FAIL")),
    INTERVIEW_2_PASS("2차면접합격", Set.of("FINAL_REVIEW")),
    INTERVIEW_2_FAIL("2차면접탈락", Set.of()),

    // 최종
    FINAL_REVIEW("최종검토", Set.of("FINAL_PASS", "FINAL_FAIL")),
    FINAL_PASS("최종합격", Set.of()),
    FINAL_FAIL("최종불합격", Set.of());

    private final String displayName;
    private final Set<String> allowedNextSteps;

    ProcessStep(String displayName, Set<String> allowedNextSteps) {
        this.displayName = displayName;
        this.allowedNextSteps = allowedNextSteps;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<String> getAllowedNextSteps() {
        return allowedNextSteps;
    }

    /**
     * 다음 단계로 전이 가능한지 확인
     */
    public boolean canTransitionTo(ProcessStep nextStep) {
        return allowedNextSteps.contains(nextStep.name());
    }

    /**
     * 종료 상태인지 확인 (더 이상 전이 불가)
     */
    public boolean isTerminal() {
        return allowedNextSteps.isEmpty();
    }

    /**
     * 합격 상태인지 확인
     */
    public boolean isPass() {
        return this == FINAL_PASS;
    }

    /**
     * 탈락 상태인지 확인
     */
    public boolean isFail() {
        return this == DOCUMENT_FAIL || this == CODING_FAIL || 
               this == INTERVIEW_1_FAIL || this == INTERVIEW_2_FAIL || 
               this == FINAL_FAIL;
    }

    /**
     * 진행 중인 상태인지 확인
     */
    public boolean isInProgress() {
        return !isTerminal();
    }
}
