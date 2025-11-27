package dev.woori.wooriBank.domain.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 계좌 개설 요청 DTO
 */
public record AccountCreateReqDto(
        @NotBlank(message = "TID는 필수입니다")
        String tid,

        @NotBlank(message = "비밀번호는 필수입니다")
        @Pattern(regexp = "^\\d{4}$", message = "비밀번호는 4자리 숫자여야 합니다")
        String password,

        @NotBlank(message = "Redirect URL은 필수입니다")
        String redirectUrl
) {
}
