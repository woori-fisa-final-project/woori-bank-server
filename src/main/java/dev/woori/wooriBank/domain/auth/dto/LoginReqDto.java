package dev.woori.wooriBank.domain.auth.dto;

public record LoginReqDto(
    String userId,
    String password
) {
}
