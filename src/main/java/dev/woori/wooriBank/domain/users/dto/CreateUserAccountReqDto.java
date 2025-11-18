package dev.woori.wooriBank.domain.users.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * 회원 생성 + 계좌 개설 요청 DTO
 */
public record CreateUserAccountReqDto(

        @NotBlank(message = "한글 이름은 필수입니다")
        @Size(max = 30, message = "한글 이름은 최대 30자입니다")
        String nameKr,

        @Size(max = 50, message = "영문 이름은 최대 50자입니다")
        String nameEn,

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        @Size(max = 50, message = "이메일은 최대 50자입니다")
        String email,

        @NotBlank(message = "전화번호는 필수입니다")
        @Size(max = 20, message = "전화번호는 최대 20자입니다")
        String phoneNumber,

        @NotNull(message = "생년월일은 필수입니다")
        LocalDate birth,

        @NotBlank(message = "계좌번호는 필수입니다")
        @Size(max = 20, message = "계좌번호는 최대 20자입니다")
        String accountNumber,

        @NotBlank(message = "계좌 비밀번호는 필수입니다")
        @Size(min = 4, max = 4, message = "계좌 비밀번호는 4자리입니다")
        String accountPassword,

        @NotNull(message = "초기 입금액은 필수입니다")
        @Min(value = 0, message = "초기 입금액은 0원 이상이어야 합니다")
        Integer initialBalance
) {
}
