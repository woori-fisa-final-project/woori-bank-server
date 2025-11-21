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

        @NotBlank(message = "계좌 PIN은 필수입니다")
        @Pattern(regexp = "^\\d{4}$", message = "계좌 PIN은 4자리 숫자여야 합니다")
        String accountPin,

        @NotNull(message = "초기 입금액은 필수입니다")
        @DecimalMin(value = "0.0", inclusive = true, message = "초기 입금액은 0 이상이어야 합니다")
        java.math.BigDecimal initialBalance
) {
}
