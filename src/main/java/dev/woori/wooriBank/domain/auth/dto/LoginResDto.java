package dev.woori.wooriBank.domain.auth.dto;

public record LoginResDto(
        String accessToken,
        String refreshToken
) {
}
