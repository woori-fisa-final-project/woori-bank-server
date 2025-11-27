package dev.woori.wooriBank.domain.auth.dto;

public record AuthVerifyReqDto(
        String tid,
        String authCode
) {
}
