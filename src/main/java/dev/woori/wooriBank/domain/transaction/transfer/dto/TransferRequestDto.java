package dev.woori.wooriBank.domain.transaction.transfer.dto;

import lombok.Getter;
import lombok.Setter;
/**
 * 입금 요청 데이터를 담는 DTO
 * 프론트 또는 관리자 API에서 요청이 올 때,
 * JSON 데이터를 이 객체로 매핑하여 사용한다.
 *
 * 예시 요청 JSON:
 * {
 *    "accountNumber": "1234567890",
 *    "amount": 50000
 * }
 */
@Getter
@Setter
public class TransferRequestDto {
    private String fromAccount; // 관리자 or 포인트 시스템 계좌
    private String toAccount;   // 사용자 계좌
    private int amount;        // 입금 금액
}
