package dev.woori.wooriBank.domain.account.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * 추가 정보 입력 요청 DTO
 */
public record AccountFormReqDto(
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        @Size(max = 50, message = "이메일은 최대 50자입니다")
        String email,

        @Size(max = 50, message = "영문 이름은 최대 50자입니다")
        String nameEn,

        @NotNull(message = "초기 입금액은 필수입니다")
        @DecimalMin(value = "0.0", inclusive = true, message = "초기 입금액은 0 이상이어야 합니다")
        BigDecimal initialBalance
) {
}
