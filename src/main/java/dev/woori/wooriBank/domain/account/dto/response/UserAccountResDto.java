package dev.woori.wooriBank.domain.account.dto.response;

import java.math.BigDecimal;

/**
 * 회원 생성 + 계좌 개설 응답 DTO
 */
public record UserAccountResDto(
        Long userId,
        String nameKr,
        String email,
        String phoneNumber,
        Long accountId,
        String accountNumber,
        BigDecimal balance
) {
}
