package dev.woori.wooriBank.domain.account.repository;

import dev.woori.wooriBank.domain.account.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    /**
     * 계좌번호로 계좌 조회
     */
    Optional<BankAccount> findByAccountNumber(String accountNumber);

    /**
     * 계좌번호 존재 여부 확인
     */
    boolean existsByAccountNumber(String accountNumber);
}
