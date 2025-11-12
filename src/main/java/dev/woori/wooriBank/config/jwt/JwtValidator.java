package dev.woori.wooriBank.config.jwt;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.domain.auth.entity.Role;
import dev.woori.wooriBank.domain.auth.repository.BankClientAppRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class JwtValidator {

    private final SecretKey secretKey;
    private final BankClientAppRepository bankClientAppRepository;

    public JwtValidator(@Value("${spring.jwt.secret}") String secret,
                        BankClientAppRepository bankClientAppRepository) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.bankClientAppRepository = bankClientAppRepository;
    }

    public String getUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public Role getRole(String token) {
        String role = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
        try{
            return Role.valueOf(role);
        }catch (IllegalArgumentException e){
            throw new CommonException(ErrorCode.INVALID_REQUEST);
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.warn("[JwtUtil] Token validation failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    public boolean validateAppKey(String appKey) {
        return bankClientAppRepository.findByAppKey(appKey).isPresent();
    }
}
