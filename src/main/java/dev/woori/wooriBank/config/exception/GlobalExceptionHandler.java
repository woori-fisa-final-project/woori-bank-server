package dev.woori.wooriBank.config.exception;

import dev.woori.wooriBank.config.response.ApiResponse;
import dev.woori.wooriBank.config.response.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 내부에서 처리할 에러
    @ExceptionHandler(CommonException.class)
    public ResponseEntity<BaseResponse<?>> handleCommonException(CommonException ex, HttpServletRequest request) {
        log.warn("[CommonException - {}] {} {} - {}",
                ex.getErrorCode(),
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());
        return ApiResponse.failure(ex.getErrorCode(), ex.getMessage());
    }

    // @Valid를 통한 유효성 검증 실패 시
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<?>> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errorMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("[ValidationException] {} {} - {}", request.getMethod(), request.getRequestURI(), errorMessage);
        return ApiResponse.failure(ErrorCode.INVALID_REQUEST, errorMessage);
    }

    // 그 외의 에러
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<?>> handleException(Exception ex, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        log.error("[CommonException - {}] {} {} - {}",
                errorCode,
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());
        return ApiResponse.failure(errorCode);
    }

}
