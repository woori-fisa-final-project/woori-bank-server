package dev.woori.wooriBank.domain.users.repository;

import dev.woori.wooriBank.domain.users.entity.BankUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankUserRepository extends JpaRepository<BankUser, Long> {
    /**
     * 전화번호 존재 여부 확인 (중복 가입 방지)
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);

    Optional<BankUser> findByRrn(String rrn);
}
