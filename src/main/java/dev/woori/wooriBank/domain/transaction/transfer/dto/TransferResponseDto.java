package dev.woori.wooriBank.domain.transaction.transfer.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 입금 처리 후 클라이언트에게 응답하는 DTO
 * 입금 완료 후 계좌번호, 입금 후 잔액, 메시지를 포함한다.
 */

@Builder
public record TransferResponseDto (
        String fromAccount, // 보내는 계좌 (관리자 계좌)
        String toAccount, // 받는 계좌 (사용자 계좌)
        Integer amount, // 이체 금액
        String message // 성공 메세지 또는 안내문구
){

}
