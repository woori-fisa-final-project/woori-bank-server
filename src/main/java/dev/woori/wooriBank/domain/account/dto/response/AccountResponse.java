package dev.woori.wooriBank.domain.account.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 계좌 개설 응답 DTO
 * 명세서: POST /api/account/create 응답
 */
public record AccountResponse(
        String accountNumber,
        String accountName,
        BigDecimal balance,
        LocalDateTime createdAt
) {
}
