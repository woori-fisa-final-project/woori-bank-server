package dev.woori.wooriBank.domain.auth.dto;

public record AuthReqDto(
        String tid,
        String name,
        String phone,
        String birth
) {
}
