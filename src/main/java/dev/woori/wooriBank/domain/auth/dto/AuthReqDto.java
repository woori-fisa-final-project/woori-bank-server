package dev.woori.wooriBank.domain.auth.dto;

public record AuthReqDto(
        String name,
        String phone,
        String birth
) {
}
