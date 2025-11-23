package dev.woori.wooriBank.domain.account.repository;

import dev.woori.wooriBank.domain.account.entity.BankAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    Optional<BankAccount> findByAccountNumber(String accountNumber);

    /**
     * 계좌번호로 계좌 조회
     *
     * Optional로 감싼 이유
     * -> 계좌가 존재하지 않을 수도 있기 때문에 예외처리를 위해서
     *
     * @Lock (비관적 락 적용)
     * - 동시에 같은 계좌를 수정하는 것을 방지
     * -> 동시 이체 요청 시 잔액 불일치 / 중복 이체 방지
     * @Query
     * -> 명시적으로 JPQL 작성 ( 자동 메서드 네이밍보다 명확)
     *  JPA 메서드 네이밍 규칙 기반 쿼리가 내부적으로 락 옵셥을 명확히 적용하지 못하거나, 상황에 따라 DB 벤더별 쿼리가 달라질 수 있기 때문에 사용
     *
     * => 잔액 차감/증가 등 '동시성 충돌 위험'이 있는 수정 트랜잭션에서만 사용
     *
     * @param accountNumber 계좌번호
     * @return Optional<BankAccount>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BankAccount b WHERE b.accountNumber = :accountNumber")
    Optional<BankAccount> findAndLockByAccountNumber(@Param("accountNumber") String accountNumber);
}
