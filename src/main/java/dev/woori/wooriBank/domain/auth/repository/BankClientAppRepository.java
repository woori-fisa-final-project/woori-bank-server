package dev.woori.wooriBank.domain.auth.repository;

import dev.woori.wooriBank.domain.auth.entity.BankClientApp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankClientAppRepository extends JpaRepository<BankClientApp, Long> {
    Optional<BankClientApp> findByAppKey(String appKey);
}
