package dev.woori.wooriBank.domain.account.entity;

import dev.woori.wooriBank.config.BaseEntity;
import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.domain.users.entity.BankUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "bank_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BankAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private BankUser user;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(nullable = false, length = 60)
    private String password;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    /**
     * 입금 처리
     * @param amount 입금액 (BigDecimal)
     * @throws CommonException amount가 null이거나 0 미만일 때
     */
    public void deposit(BigDecimal amount) {
        validateAmount(amount, "입금액");
        this.balance = this.balance.add(amount);
    }

    /**
     * 출금 처리
     * @param amount 출금액 (BigDecimal)
     * @throws CommonException amount가 null이거나 0 미만일 때, 또는 잔액 부족 시
     */
    public void withdraw(BigDecimal amount) {
        validateAmount(amount, "출금액");
        if (this.balance.compareTo(amount) < 0) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "잔액이 부족합니다.");
        }
        this.balance = this.balance.subtract(amount);
    }

    /**
     * 금액 유효성 검증
     * @param amount 검증할 금액
     * @param amountType 금액 종류 (입금액/출금액)
     * @throws CommonException amount가 null이거나 0 이하일 때
     */
    private void validateAmount(BigDecimal amount, String amountType) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, amountType + "은 0 이상이어야 합니다.");
        }
    }
}