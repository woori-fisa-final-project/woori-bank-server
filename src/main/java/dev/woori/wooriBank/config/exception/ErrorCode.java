package dev.woori.wooriBank.config.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 에러 통합 관리
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, 400, "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 401, "인증이 필요합니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, 401, "토큰이 만료되었습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, 403, "접근 권한이 없습니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "대상을 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, 409, "이미 존재하는 ID입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 500, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;
}
