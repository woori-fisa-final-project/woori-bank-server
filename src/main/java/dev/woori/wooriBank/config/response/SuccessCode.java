package dev.woori.wooriBank.config.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessCode {
    OK(HttpStatus.OK, 200, "요청이 성공했습니다."),
    CREATED(HttpStatus.CREATED, 201, "요청이 성공했습니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;
}
