package dev.woori.wooriBank.domain.account.dto.response;

/**
 * 계좌 개설 응답 DTO (Redirect URL)
 */
public record AccountCreateResDto(
        String redirectUrl
) {
}
