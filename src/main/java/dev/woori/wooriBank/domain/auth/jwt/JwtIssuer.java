package dev.woori.wooriBank.domain.auth.jwt;

import dev.woori.wooriBank.config.jwt.TokenInfo;
import dev.woori.wooriBank.domain.auth.entity.Role;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Slf4j
@Component
public class JwtIssuer {
    private final SecretKey secretKey;

    @Value("${spring.jwt.access-expiration}")
    private long accessTokenExpiration;

    @Value("${spring.jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    public JwtIssuer(@Value("${spring.jwt.secret}") String secret) {
        secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String username, Role role) {
        return generateToken(username, role, accessTokenExpiration).token();
    }

    public TokenInfo generateRefreshToken(String username, Role role) {
        return generateToken(username, role, refreshTokenExpiration);
    }

    public TokenInfo generateToken(String username, Role role, long expirationMillis){
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationMillis);

        String token = Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();

        return new TokenInfo(token, expiration);
    }
}
