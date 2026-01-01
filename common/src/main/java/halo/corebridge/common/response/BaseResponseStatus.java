package halo.corebridge.common.response;

public enum BaseResponseStatus {

    // Success
    SUCCESS(true, 1000, "요청에 성공했습니다."),

    // Common Error
    INVALID_REQUEST(false, 2000, "잘못된 요청입니다."),
    UNAUTHORIZED(false, 2001, "인증이 필요합니다."),
    FORBIDDEN(false, 2002, "접근 권한이 없습니다."),
    NOT_FOUND(false, 2003, "리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(false, 2004, "서버 내부 오류입니다.");

    private final boolean success;
    private final int code;
    private final String message;

    BaseResponseStatus(boolean success, int code, String message) {
        this.success = success;
        this.code = code;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
