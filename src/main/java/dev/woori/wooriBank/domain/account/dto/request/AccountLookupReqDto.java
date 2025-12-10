package dev.woori.wooriBank.domain.account.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 계좌 조회 요청 DTO (Code 기반)
 */
public record AccountLookupReqDto(
        @NotBlank(message = "Code는 필수입니다")
        String code
) {
}
