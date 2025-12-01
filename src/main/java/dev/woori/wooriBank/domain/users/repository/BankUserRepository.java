package dev.woori.wooriBank.domain.users.repository;

import dev.woori.wooriBank.domain.users.entity.BankUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankUserRepository extends JpaRepository<BankUser, Long> {
    Optional<BankUser> findByRrn(String rrn);
}
