package dev.woori.wooriBank.domain.account.dto.request;

import jakarta.validation.constraints.*;

/**
 * 계좌 개설 요청 DTO
 * 명세서: POST /api/account/create
 */
public record AccountCreateDto(

        @NotBlank(message = "이름은 필수입니다")
        @Size(min = 2, max = 20, message = "이름은 2~20자여야 합니다")
        String name,

        @NotBlank(message = "전화번호는 필수입니다")
        @Pattern(regexp = "^010\\d{8}$", message = "올바른 전화번호 형식이 아닙니다")
        String phone,

        @NotBlank(message = "생년월일은 필수입니다")
        @Pattern(regexp = "^\\d{8}$", message = "생년월일은 YYYYMMDD 형식이어야 합니다")
        String birth,

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String email,

        @NotBlank(message = "소속 기관은 필수입니다")
        @Size(min = 2, max = 50, message = "소속 기관은 2~50자여야 합니다")
        String orgName,

        @NotBlank(message = "계좌 PIN은 필수입니다")
        @Pattern(regexp = "^\\d{4}$", message = "계좌 PIN은 4자리 숫자여야 합니다")
        String password
) {
}
