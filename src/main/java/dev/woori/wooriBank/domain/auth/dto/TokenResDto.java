package dev.woori.wooriBank.domain.auth.dto;

public record TokenResDto(
        String accessToken,
        String refreshToken
) {
}
