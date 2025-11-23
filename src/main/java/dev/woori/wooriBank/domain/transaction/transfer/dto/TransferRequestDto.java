package dev.woori.wooriBank.domain.transaction.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

/**
 * 입금 요청 데이터를 담는 DTO
 * 프론트 또는 관리자 API에서 요청이 올 때,
 * JSON 데이터를 이 객체로 매핑하여 사용한다.
 *
 *  @NotBlank → String 공백/빈값 방지
 *  @Positive → 금액은 양수여야 함
 */
@Builder
public record TransferRequestDto (
        @NotBlank(message = "보내는 계좌번호는 필수입니다.")
        String fromAccount, // 관리자 or 포인트 시스템 계좌

        @NotBlank(message = "받는 계좌번호는 필수입니다.")
        String toAccount,  // 사용자 계좌

        @Positive(message = "이체 금액은 0보다 커야 합니다.")
        int amount        // 입금 금액
){

}
