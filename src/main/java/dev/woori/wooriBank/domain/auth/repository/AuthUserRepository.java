package dev.woori.wooriBank.domain.auth.repository;

import dev.woori.wooriBank.domain.auth.entity.AuthUsers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthUserRepository extends JpaRepository<AuthUsers, Long> {

    Optional<AuthUsers> findByUserId(String userId);

    Boolean existsByUserId(String userId);
}
