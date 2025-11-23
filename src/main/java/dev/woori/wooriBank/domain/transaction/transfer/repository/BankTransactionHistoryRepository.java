package dev.woori.wooriBank.domain.transaction.transfer.repository;

import dev.woori.wooriBank.domain.transaction.entity.BankTransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 거래내역(BankTransactionHistory) 엔티티를 DB와 연결시키는 Repository 인터페이스.
 *
 * JpaRepository를 상속받으면 자동으로 기본 CRUD(저장, 조회 등) 기능을 사용할 수 있다.
 */

public interface BankTransactionHistoryRepository
        extends JpaRepository<BankTransactionHistory, Long> {
}
