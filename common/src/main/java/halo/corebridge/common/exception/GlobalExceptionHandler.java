package halo.corebridge.common.exception;

import halo.corebridge.common.response.BaseResponse;
import halo.corebridge.common.response.BaseResponseStatus;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    protected ResponseEntity<BaseResponse<Void>> handleBaseException(BaseException e) {
        BaseResponseStatus status = e.getStatus();
        return ResponseEntity
                .status(status.getHttpStatus())
                .body(BaseResponse.failure(status));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<BaseResponse<Void>> handleException(Exception e) {
        return ResponseEntity
                .status(BaseResponseStatus.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(BaseResponse.failure(BaseResponseStatus.INTERNAL_SERVER_ERROR));
    }
}
