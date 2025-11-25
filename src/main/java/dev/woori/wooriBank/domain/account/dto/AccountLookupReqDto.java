package dev.woori.wooriBank.domain.account.dto;

import jakarta.validation.constraints.NotBlank;

public record AccountLookupReqDto(
        @NotBlank String id,
        @NotBlank String code
) {
}
