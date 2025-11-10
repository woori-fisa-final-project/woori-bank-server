package dev.woori.wooriBank.domain.auth.port;

import dev.woori.wooriBank.domain.auth.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenPort {
    Optional<RefreshToken> findByUsername(String username);

    void deleteByUsername(String username);

    RefreshToken save(RefreshToken token);
}
