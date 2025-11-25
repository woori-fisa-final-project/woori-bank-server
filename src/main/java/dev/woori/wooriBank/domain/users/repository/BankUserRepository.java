package dev.woori.wooriBank.domain.users.repository;

import dev.woori.wooriBank.domain.users.entity.BankUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankUserRepository extends JpaRepository<BankUser, Long> {

    /**
     * 이메일로 사용자 조회
     */
    Optional<BankUser> findByEmail(String email);

    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);

    /**
     * 메인 서버 userId(authToken)로 사용자 조회
     */
    Optional<BankUser> findByAuthToken(String authToken);

    /**
     * 메인 서버 userId(authToken) 존재 여부 확인 (1인 1계좌 체크용)
     */
    boolean existsByAuthToken(String authToken);
}
