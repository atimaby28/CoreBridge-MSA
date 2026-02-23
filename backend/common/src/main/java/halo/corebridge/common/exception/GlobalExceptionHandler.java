package halo.corebridge.common.exception;

import halo.corebridge.common.response.BaseResponse;
import halo.corebridge.common.response.BaseResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * BaseException 처리
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<BaseResponse<Void>> handleBaseException(BaseException e) {
        log.warn("BaseException: {}", e.getMessage());
        return ResponseEntity
                .status(mapToHttpStatus(e.getStatus()))
                .body(BaseResponse.failure(e.getStatus()));
    }

    /**
     * Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", errorMessage);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.failure(BaseResponseStatus.INVALID_REQUEST.getCode(), errorMessage));
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("IllegalArgumentException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.failure(BaseResponseStatus.INVALID_REQUEST.getCode(), e.getMessage()));
    }

    /**
     * 그 외 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleException(Exception e) {
        log.error("Unexpected error: ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.failure(BaseResponseStatus.INTERNAL_SERVER_ERROR));
    }

    /**
     * BaseResponseStatus를 HttpStatus로 매핑
     */
    private HttpStatus mapToHttpStatus(BaseResponseStatus status) {
        return switch (status) {
            case UNAUTHORIZED, INVALID_TOKEN, EXPIRED_TOKEN -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND, USER_NOT_FOUND, JOBPOSTING_NOT_FOUND, 
                 APPLICATION_NOT_FOUND, PROCESS_NOT_FOUND, 
                 RESUME_NOT_FOUND, SCHEDULE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_REQUEST, INVALID_PASSWORD, DUPLICATE_EMAIL,
                 USER_ALREADY_EXISTS, ALREADY_APPLIED, 
                 INVALID_STATUS_TRANSITION, SCHEDULE_CONFLICT -> HttpStatus.BAD_REQUEST;
            case JOBPOSTING_CLOSED, APPLICATION_CLOSED -> HttpStatus.GONE;
            case NOT_JOBPOSTING_OWNER -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
