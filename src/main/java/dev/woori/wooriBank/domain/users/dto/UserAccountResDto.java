package dev.woori.wooriBank.domain.users.dto;

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
        Integer balance
) {
}
