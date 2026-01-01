package halo.corebridge.user.exception;

import halo.corebridge.common.response.BaseResponse;
import halo.corebridge.common.response.BaseResponseStatus;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Log4j2
@RestControllerAdvice
public class GlobalExceptionHandler {

    /* =========================
       User 도메인 예외
       ========================= */

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleUserNotFound(
            UserNotFoundException e
    ) {
        log.warn("[USER_NOT_FOUND] {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        BaseResponse.failure(BaseResponseStatus.NOT_FOUND)
                );
    }

    /* =========================
       잘못된 요청
       ========================= */

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseResponse<Void>> handleIllegalArgument(
            IllegalArgumentException e
    ) {
        log.warn("[INVALID_REQUEST] {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        BaseResponse.failure(BaseResponseStatus.INVALID_REQUEST)
                );
    }

    /* =========================
       예상하지 못한 서버 오류
       ========================= */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleException(
            Exception e
    ) {
        log.error("[INTERNAL_SERVER_ERROR]", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        BaseResponse.failure(BaseResponseStatus.INTERNAL_SERVER_ERROR)
                );
    }
}
