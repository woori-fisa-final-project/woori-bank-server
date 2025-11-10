package dev.woori.wooriBank.domain.auth.repository;

import dev.woori.wooriBank.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 추후 redis 등의 캐시 저장소로 변경할 예정
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByUsername(String username);

    void deleteByUsername(String username);
}
