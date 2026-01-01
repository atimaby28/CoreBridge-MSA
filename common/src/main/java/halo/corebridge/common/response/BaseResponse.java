package halo.corebridge.common.response;

import lombok.Getter;

@Getter
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

    // 성공 (데이터 있음)
    public static <T> BaseResponse<T> success(T result) {
        return new BaseResponse<>(BaseResponseStatus.SUCCESS, result);
    }

    // 성공 (데이터 없음)
    public static BaseResponse<Void> success() {
        return new BaseResponse<>(BaseResponseStatus.SUCCESS, null);
    }

    // 실패
    public static BaseResponse<Void> failure(BaseResponseStatus status) {
        return new BaseResponse<>(status, null);
    }
}
