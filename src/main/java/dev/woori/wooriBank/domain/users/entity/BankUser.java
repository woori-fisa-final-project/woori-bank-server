package dev.woori.wooriBank.domain.users.entity;

import dev.woori.wooriBank.config.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "bank_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BankUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_kr", nullable = false, length = 30)
    private String nameKr;

    @Column(name = "name_en", length = 50)
    private String nameEn;

    @Column(nullable = false, length = 50)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false)
    private LocalDate birth;

    @Column(name = "auth_token", nullable = false, length = 255)
    private String authToken;
}
