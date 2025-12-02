package dev.woori.wooriBank.domain.users.entity;

import dev.woori.wooriBank.config.BaseEntity;
import dev.woori.wooriBank.domain.util.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 은행 사용자 엔티티
 *
 * 보안: 민감 정보는 DB에 암호화되어 저장됩니다.
 * - rrn: 주민등록번호 (AES-256-GCM 암호화)
 * - nameKr: 한글 이름 (AES-256-GCM 암호화)
 * - phoneNumber: 전화번호 (AES-256-GCM 암호화)
 */
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

    @Column(name = "registration_number", nullable = false, length = 255)
    @Convert(converter = EncryptedStringConverter.class)
    private String rrn;

    /**
     * RRN 해시값 (검색용)
     * SHA-256 해시로 저장하여 암호화된 RRN 조회 가능
     */
    @Column(name = "rrn_hash", nullable = false, unique = true, length = 64)
    private String rrnHash;

    @Column(name = "name_kr", nullable = false, length = 255)
    @Convert(converter = EncryptedStringConverter.class)
    private String nameKr;

    @Column(name = "name_en", length = 50)
    private String nameEn;

    @Column(nullable = false, length = 50)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 255)
    @Convert(converter = EncryptedStringConverter.class)
    private String phoneNumber;

    @Column(nullable = false)
    private LocalDate birth;
}
