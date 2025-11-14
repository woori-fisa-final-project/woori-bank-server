package dev.woori.wooriBank.config.jwt;

import dev.woori.wooriBank.domain.auth.entity.Role;

public record JwtInfo(
        String username,
        Role role
) {
}
