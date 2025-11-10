package dev.woori.wooriBank.config.response;

import dev.woori.wooriBank.config.exception.ErrorCode;
import org.springframework.http.ResponseEntity;

public class ApiResponse {
    public static <T> ResponseEntity<BaseResponse<?>> success(final SuccessCode successCode) {
        return ResponseEntity.status(successCode.getStatus())
                .body(BaseResponse.of(successCode));
    }

    public static <T> ResponseEntity<BaseResponse<?>> success(final SuccessCode successCode, final T data) {
        return ResponseEntity.status(successCode.getStatus())
                .body(BaseResponse.of(successCode, data));
    }

    public static ResponseEntity<BaseResponse<?>> failure(final ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(BaseResponse.of(errorCode));
    }

    // 따로 에러 메시지를 넣어줄 때 사용
    public static ResponseEntity<BaseResponse<?>> failure(final ErrorCode errorCode, final String message) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(BaseResponse.of(errorCode, message));
    }
}
