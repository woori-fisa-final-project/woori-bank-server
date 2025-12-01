package dev.woori.wooriBank.domain.account.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 계좌 개설 추가 정보 입력 요청 DTO
 */
public record AccountFormReqDto(
        @NotBlank(message = "TID는 필수입니다")
        String tid,

        @NotBlank(message = "영어 이름은 필수입니다")
        @Size(min = 2, max = 50, message = "영어 이름은 2~50자여야 합니다")
        String engName,

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "이메일 형식이 올바르지 않습니다")
        String email
) {
}
