package dev.woori.wooriBank.domain.account.entity;

import dev.woori.wooriBank.config.BaseEntity;
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

    @Column(nullable = false, length = 4)
    private String password;

    @Column(nullable = false)
    private Integer balance;

    public void deposit(int amount) {
        this.balance += amount;
    }

    public void withdraw(int amount) {
        this.balance -= amount;
    }
}