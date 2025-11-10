package dev.woori.wooriBank.config.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.woori.wooriBank.config.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class BaseResponse<T> {
    @JsonProperty("code")
    private int code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    public static BaseResponse<?> of(SuccessCode successCode) {
        return BaseResponse.builder()
                .code(successCode.getCode())
                .message(successCode.getMessage())
                .build();
    }

    public static <T> BaseResponse<?> of(SuccessCode successCode, T data) {
        return BaseResponse.builder()
                .code(successCode.getCode())
                .message(successCode.getMessage())
                .data(data)
                .build();
    }

    public static <T> BaseResponse<?> of(ErrorCode errorCode) {
        return BaseResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }

    public static <T> BaseResponse<?> of(ErrorCode errorCode, String message) {
        return BaseResponse.builder()
                .code(errorCode.getCode())
                .message(message)
                .build();
    }
}
