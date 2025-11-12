package dev.woori.wooriBank.domain.auth.entity;

import dev.woori.wooriBank.config.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "bank_client_app")
public class BankClientApp extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "app_key", nullable = false, unique = true)
    private String appKey;

    @Column(name = "secret_key", nullable = false)
    private String secretKey;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    public enum Status {
        ACTIVE, INACTIVE
    }
}