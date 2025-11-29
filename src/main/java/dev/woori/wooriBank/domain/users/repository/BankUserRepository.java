package dev.woori.wooriBank.domain.users.repository;

import dev.woori.wooriBank.domain.users.entity.BankUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankUserRepository extends JpaRepository<BankUser, Long> {

    /**
     * 전화번호로 사용자 조회
     */
    Optional<BankUser> findByPhoneNumber(String phoneNumber);

    /**
     * 전화번호 존재 여부 확인 (중복 가입 방지)
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * 이메일로 사용자 조회
     */
    Optional<BankUser> findByEmail(String email);

    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);

    /**
     * TID로 사용자 조회
     */
    Optional<BankUser> findByAccountCreationTid(String tid);

    /**
     * TID 존재 여부 확인 (1인 1계좌 체크)
     */
    boolean existsByAccountCreationTid(String tid);
}
