package dev.woori.wooriBank.domain.account.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * 약관 동의 요청 DTO
 */
public record TermsSubmitReqDto(
        @NotNull(message = "약관 동의 여부는 필수입니다")
        @AssertTrue(message = "약관에 동의해야 합니다")
        Boolean termsAgreed
) {
}
