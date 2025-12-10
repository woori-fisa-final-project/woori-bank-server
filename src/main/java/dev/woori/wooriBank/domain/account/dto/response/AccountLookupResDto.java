package dev.woori.wooriBank.domain.account.dto.response;

/**
 * 계좌 조회 응답 DTO
 */
public record AccountLookupResDto(
        String name,           // 사용자 이름
        String accountNumber  // 계좌번호
) {
}
