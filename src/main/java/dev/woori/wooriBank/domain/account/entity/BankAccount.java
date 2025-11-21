package dev.woori.wooriBank.domain.account.entity;

import dev.woori.wooriBank.config.BaseEntity;
import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.domain.users.entity.BankUser;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, precision = 15, scale = 2)
    private java.math.BigDecimal balance;

    public void deposit(java.math.BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void withdraw(java.math.BigDecimal amount) {
        if(this.balance.compareTo(amount) < 0) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "잔액이 부족합니다.");
        }
        this.balance = this.balance.subtract(amount);
    }
}