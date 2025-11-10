package dev.woori.wooriBank.config.jwt;

import java.time.Instant;

public record TokenInfo(
        String token,
        Instant expiration
) {
}
