package halo.corebridge.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {

    private final boolean isSuccess;
    private final int code;
    private final String message;
    private final T result;

    private BaseResponse(BaseResponseStatus status, T result) {
        this.isSuccess = status.isSuccess();
        this.code = status.getCode();
        this.message = status.getMessage();
        this.result = result;
    }

    private BaseResponse(boolean isSuccess, int code, String message, T result) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
        this.result = result;
    }

    // ========== 성공 응답 ==========

    /**
     * 성공 응답 (데이터 있음)
     */
    public static <T> BaseResponse<T> success(T result) {
        return new BaseResponse<>(BaseResponseStatus.SUCCESS, result);
    }

    /**
     * 성공 응답 (데이터 없음)
     */
    public static BaseResponse<Void> success() {
        return new BaseResponse<>(BaseResponseStatus.SUCCESS, null);
    }

    /**
     * 성공 응답 (커스텀 메시지)
     */
    public static <T> BaseResponse<T> success(String message, T result) {
        return new BaseResponse<>(true, 1000, message, result);
    }

    // ========== 실패 응답 ==========

    /**
     * 실패 응답 (status 기반)
     */
    public static BaseResponse<Void> failure(BaseResponseStatus status) {
        return new BaseResponse<>(status, null);
    }

    /**
     * 실패 응답 (코드/메시지 기반)
     */
    public static BaseResponse<Void> failure(int code, String message) {
        return new BaseResponse<>(false, code, message, null);
    }

    /**
     * 실패 응답 (데이터 포함)
     */
    public static <T> BaseResponse<T> failure(BaseResponseStatus status, T result) {
        return new BaseResponse<>(status, result);
    }
}
