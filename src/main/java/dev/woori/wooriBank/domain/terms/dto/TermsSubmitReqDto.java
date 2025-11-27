package dev.woori.wooriBank.domain.terms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 약관 동의 제출 요청 DTO
 */
public record TermsSubmitReqDto(
        @NotNull(message = "TID는 필수입니다")
        String tid,

        @NotEmpty(message = "약관 동의 항목은 필수입니다")
        @Valid
        List<TermAgreement> terms
) {
    public record TermAgreement(
            @NotNull(message = "약관 ID는 필수입니다")
            String termId,

            @NotNull(message = "동의 여부는 필수입니다")
            Boolean agreed
    ) {}
}
