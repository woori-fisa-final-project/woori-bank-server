package dev.woori.wooriBank.domain.transaction.entity;

import dev.woori.wooriBank.domain.account.entity.BankAccount;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "bank_transaction_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class BankTransactionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private BankAccount account;

    @CreatedDate // 자동 생성
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "counterparty_name", nullable = false, length = 30)
    private String counterpartyName;

    @Column(name = "display_name", length = 30)
    private String displayName;

    @Column(nullable = false, precision = 15, scale = 2)
    private java.math.BigDecimal amount;

    @Column(length = 50)
    private String description;
}