package halo.corebridge.notification.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    // 채용 프로세스 관련
    PROCESS_UPDATE("채용 진행 상태 변경"),
    DOCUMENT_PASS("서류 전형 합격"),
    DOCUMENT_FAIL("서류 전형 불합격"),
    CODING_TEST_SCHEDULED("코딩 테스트 일정 안내"),
    CODING_TEST_PASS("코딩 테스트 합격"),
    CODING_TEST_FAIL("코딩 테스트 불합격"),
    INTERVIEW_SCHEDULED("면접 일정 안내"),
    INTERVIEW_PASS("면접 합격"),
    INTERVIEW_FAIL("면접 불합격"),
    FINAL_PASS("최종 합격"),
    FINAL_FAIL("최종 불합격"),

    // 지원 관련
    APPLY_RECEIVED("지원서 접수 완료"),
    APPLY_CANCELLED("지원 취소"),

    // 이력서 관련
    RESUME_ANALYSIS_COMPLETE("이력서 AI 분석 완료"),

    // 시스템 알림
    SYSTEM("시스템 알림");

    private final String description;
}
