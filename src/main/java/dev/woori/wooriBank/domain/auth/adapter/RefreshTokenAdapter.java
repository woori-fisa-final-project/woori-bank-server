package dev.woori.wooriBank.domain.auth.adapter;

import dev.woori.wooriBank.domain.auth.entity.RefreshToken;
import dev.woori.wooriBank.domain.auth.port.RefreshTokenPort;
import dev.woori.wooriBank.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenAdapter implements RefreshTokenPort {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public Optional<RefreshToken> findByUsername(String username) {
        return refreshTokenRepository.findByUsername(username);
    }

    @Override
    public void deleteByUsername(String username) {
        refreshTokenRepository.deleteByUsername(username);
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return refreshTokenRepository.save(token);
    }
}
