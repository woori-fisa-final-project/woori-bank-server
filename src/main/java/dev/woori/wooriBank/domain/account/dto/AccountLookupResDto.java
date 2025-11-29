package dev.woori.wooriBank.domain.account.dto;

/**
 * 계좌 조회 응답 DTO
 */
public record AccountLookupResDto(
        Long userId,           // 포인트 연동을 위한 사용자 ID
        String name,           // 사용자 이름
        String accountNumber,  // 계좌번호
        String email,          // 이메일
        String orgName         // 소속 기관
) {
}
