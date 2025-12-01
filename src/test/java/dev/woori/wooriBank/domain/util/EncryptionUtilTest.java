package dev.woori.wooriBank.domain.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

/**
 * EncryptionUtil 테스트
 * AES-256-GCM 암호화/복호화 검증
 */
class EncryptionUtilTest {

    /**
     * 테스트용 암호화 키 생성 (32바이트)
     */
    private EncryptionUtil createEncryptionUtil() {
        // 32바이트 키 생성
        byte[] key = new byte[32];
        for (int i = 0; i < 32; i++) {
            key[i] = (byte) i;
        }
        String encodedKey = Base64.getEncoder().encodeToString(key);
        return new EncryptionUtil(encodedKey);
    }

    @Test
    @DisplayName("암호화 후 복호화하면 원본 데이터와 일치해야 한다")
    void encryptAndDecrypt_ShouldReturnOriginalText() {
        // given
        EncryptionUtil encryptionUtil = createEncryptionUtil();
        String plainText = "김철수";

        // when
        String encrypted = encryptionUtil.encrypt(plainText);
        String decrypted = encryptionUtil.decrypt(encrypted);

        // then
        assertThat(decrypted).isEqualTo(plainText);
        assertThat(encrypted).isNotEqualTo(plainText); // 암호화되었는지 확인
    }

    @Test
    @DisplayName("동일한 평문을 암호화해도 매번 다른 암호문이 생성되어야 한다 (IV가 랜덤)")
    void encrypt_ShouldGenerateDifferentCiphertext_ForSamePlaintext() {
        // given
        EncryptionUtil encryptionUtil = createEncryptionUtil();
        String plainText = "01012345678";

        // when
        String encrypted1 = encryptionUtil.encrypt(plainText);
        String encrypted2 = encryptionUtil.encrypt(plainText);

        // then
        assertThat(encrypted1).isNotEqualTo(encrypted2); // IV가 다르므로 암호문도 달라야 함
        assertThat(encryptionUtil.decrypt(encrypted1)).isEqualTo(plainText);
        assertThat(encryptionUtil.decrypt(encrypted2)).isEqualTo(plainText);
    }

    @Test
    @DisplayName("null 또는 빈 문자열은 그대로 반환되어야 한다")
    void encrypt_ShouldReturnAsIs_WhenNullOrEmpty() {
        // given
        EncryptionUtil encryptionUtil = createEncryptionUtil();

        // when & then
        assertThat(encryptionUtil.encrypt(null)).isNull();
        assertThat(encryptionUtil.encrypt("")).isEmpty();
        assertThat(encryptionUtil.decrypt(null)).isNull();
        assertThat(encryptionUtil.decrypt("")).isEmpty();
    }

    @Test
    @DisplayName("개인정보(이름, 전화번호, 생년월일) 암호화 테스트")
    void encryptPersonalInfo_ShouldWorkCorrectly() {
        // given
        EncryptionUtil encryptionUtil = createEncryptionUtil();
        String name = "김철수";
        String phone = "01012345678";
        String birth = "19900101";

        // when
        String encryptedName = encryptionUtil.encrypt(name);
        String encryptedPhone = encryptionUtil.encrypt(phone);
        String encryptedBirth = encryptionUtil.encrypt(birth);

        // then
        assertThat(encryptionUtil.decrypt(encryptedName)).isEqualTo(name);
        assertThat(encryptionUtil.decrypt(encryptedPhone)).isEqualTo(phone);
        assertThat(encryptionUtil.decrypt(encryptedBirth)).isEqualTo(birth);
    }

    @Test
    @DisplayName("키 길이가 32바이트가 아니면 예외가 발생해야 한다 - 짧은 경우")
    void constructor_ShouldThrowException_WhenKeyIsTooShort() {
        // given
        byte[] shortKey = new byte[16]; // 16바이트 (너무 짧음)
        String encodedKey = Base64.getEncoder().encodeToString(shortKey);

        // when & then
        assertThatThrownBy(() -> new EncryptionUtil(encodedKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("정확히 32바이트");
    }

    @Test
    @DisplayName("키 길이가 32바이트가 아니면 예외가 발생해야 한다 - 긴 경우")
    void constructor_ShouldThrowException_WhenKeyIsTooLong() {
        // given
        byte[] longKey = new byte[64]; // 64바이트 (너무 김)
        String encodedKey = Base64.getEncoder().encodeToString(longKey);

        // when & then
        assertThatThrownBy(() -> new EncryptionUtil(encodedKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("정확히 32바이트");
    }

    @Test
    @DisplayName("정확히 32바이트 키는 정상적으로 동작해야 한다")
    void constructor_ShouldWork_WhenKeyIsExactly32Bytes() {
        // given
        byte[] validKey = new byte[32];
        String encodedKey = Base64.getEncoder().encodeToString(validKey);

        // when & then
        assertThatCode(() -> new EncryptionUtil(encodedKey))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("한글, 영문, 숫자, 특수문자 모두 정상적으로 암호화/복호화되어야 한다")
    void encrypt_ShouldHandleVariousCharacters() {
        // given
        EncryptionUtil encryptionUtil = createEncryptionUtil();
        String mixed = "김철수 Kim 123 !@#$%^&*()";

        // when
        String encrypted = encryptionUtil.encrypt(mixed);
        String decrypted = encryptionUtil.decrypt(encrypted);

        // then
        assertThat(decrypted).isEqualTo(mixed);
    }

    @Test
    @DisplayName("매우 긴 문자열도 정상적으로 암호화/복호화되어야 한다")
    void encrypt_ShouldHandleLongText() {
        // given
        EncryptionUtil encryptionUtil = createEncryptionUtil();
        String longText = "a".repeat(10000); // 10,000자

        // when
        String encrypted = encryptionUtil.encrypt(longText);
        String decrypted = encryptionUtil.decrypt(encrypted);

        // then
        assertThat(decrypted).isEqualTo(longText);
    }
}
