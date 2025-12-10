package dev.woori.wooriBank.domain.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter for automatic encryption/decryption
 * DB 저장 시 자동 암호화, 조회 시 자동 복호화
 *
 * @see BankUser - rrn, nameKr, phoneNumber 필드에 적용
 */
@Converter
@Component
@RequiredArgsConstructor
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final EncryptionUtil encryptionUtil;

    /**
     * 엔티티 → DB 저장 (암호화)
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return encryptionUtil.encrypt(attribute);
    }

    /**
     * DB → 엔티티 조회 (복호화)
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return encryptionUtil.decrypt(dbData);
    }
}
