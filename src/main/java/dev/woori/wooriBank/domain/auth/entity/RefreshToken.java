package dev.woori.wooriBank.domain.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Column(columnDefinition = "TEXT")
    private String token;

    private Instant expiration;

    public void updateToken(String newToken, Instant newExpiration) {
        this.token = newToken;
        this.expiration = newExpiration;
    }
}
