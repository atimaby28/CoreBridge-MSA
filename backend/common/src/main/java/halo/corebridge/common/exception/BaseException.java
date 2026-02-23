package halo.corebridge.common.exception;

import halo.corebridge.common.response.BaseResponseStatus;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

    private final BaseResponseStatus status;

    public BaseException(BaseResponseStatus status) {
        super(status.getMessage());
        this.status = status;
    }

    public BaseException(BaseResponseStatus status, String message) {
        super(message);
        this.status = status;
    }

    public BaseException(BaseResponseStatus status, Throwable cause) {
        super(status.getMessage(), cause);
        this.status = status;
    }
}
