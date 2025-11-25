package dev.woori.wooriBank.domain.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 계좌 개설 요청 DTO (최종 단계 - 계좌 PIN 입력)
 */
public record AccountCreateReqDto(
        @NotBlank(message = "계좌 PIN은 필수입니다")
        @Pattern(regexp = "^\\d{4}$", message = "계좌 PIN은 4자리 숫자여야 합니다")
        String accountPin
) {
}
