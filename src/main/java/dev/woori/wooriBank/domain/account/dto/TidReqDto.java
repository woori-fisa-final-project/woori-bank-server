package dev.woori.wooriBank.domain.account.dto;

import jakarta.validation.constraints.NotBlank;

public record TidReqDto(
        @NotBlank String clientId
) {
}
