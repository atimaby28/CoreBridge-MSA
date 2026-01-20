package halo.corebridge.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BaseResponseStatus {

    // ========== Success ==========
    SUCCESS(true, 1000, "요청에 성공했습니다."),

    // ========== Common Error (2xxx) ==========
    INVALID_REQUEST(false, 2000, "잘못된 요청입니다."),
    UNAUTHORIZED(false, 2001, "인증이 필요합니다."),
    FORBIDDEN(false, 2002, "접근 권한이 없습니다."),
    NOT_FOUND(false, 2003, "리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(false, 2004, "서버 내부 오류입니다."),
    INVALID_TOKEN(false, 2005, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(false, 2006, "만료된 토큰입니다."),

    // ========== User Error (3xxx) ==========
    USER_NOT_FOUND(false, 3000, "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(false, 3001, "이미 등록된 이메일입니다."),
    INVALID_PASSWORD(false, 3002, "비밀번호가 일치하지 않습니다."),
    USER_ALREADY_EXISTS(false, 3003, "이미 존재하는 사용자입니다."),

    // ========== JobPosting Error (4xxx) ==========
    JOBPOSTING_NOT_FOUND(false, 4000, "채용공고를 찾을 수 없습니다."),
    JOBPOSTING_CLOSED(false, 4001, "마감된 채용공고입니다."),
    NOT_JOBPOSTING_OWNER(false, 4002, "채용공고 작성자가 아닙니다."),

    // ========== Application Error (5xxx) ==========
    APPLICATION_NOT_FOUND(false, 5000, "지원서를 찾을 수 없습니다."),
    ALREADY_APPLIED(false, 5001, "이미 지원한 공고입니다."),
    APPLICATION_CLOSED(false, 5002, "지원이 마감되었습니다."),

    // ========== Process Error (6xxx) ==========
    PROCESS_NOT_FOUND(false, 6000, "채용 프로세스를 찾을 수 없습니다."),
    INVALID_STATUS_TRANSITION(false, 6001, "유효하지 않은 상태 전이입니다."),

    // ========== Resume Error (7xxx) ==========
    RESUME_NOT_FOUND(false, 7000, "이력서를 찾을 수 없습니다."),

    // ========== Schedule Error (8xxx) ==========
    SCHEDULE_NOT_FOUND(false, 8000, "일정을 찾을 수 없습니다."),
    SCHEDULE_CONFLICT(false, 8001, "일정이 중복됩니다.");

    private final boolean success;
    private final int code;
    private final String message;
}
