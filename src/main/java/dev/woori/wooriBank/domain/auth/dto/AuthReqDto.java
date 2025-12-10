package dev.woori.wooriBank.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AuthReqDto(
        @NotBlank(message = "TID는 필수입니다")
        String tid,

        @NotBlank(message = "이름은 필수입니다")
        String name,

        @NotBlank(message = "전화번호는 필수입니다")
        @Pattern(regexp = "^01[0-9]{9}$", message = "전화번호는 01로 시작하는 11자리 숫자여야 합니다")
        String phone,

        @NotBlank(message = "생년월일은 필수입니다")
        @Pattern(regexp = "^\\d{8}$", message = "생년월일은 yyyyMMdd 형식의 8자리 숫자여야 합니다")
        String birth
) {
}
