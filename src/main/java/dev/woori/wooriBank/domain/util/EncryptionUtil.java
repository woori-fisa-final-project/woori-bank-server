package dev.woori.wooriBank.domain.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 암호화 유틸리티
 * 민감한 개인정보(이름, 전화번호, 생년월일 등)를 암호화하여 Redis에 저장
 */
@Slf4j
@Component
public class EncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom;

    public EncryptionUtil(@Value("${spring.jwt.secret}") String secret) {
        // JWT secret을 재사용 (Base64 디코딩 후 32바이트로 자름)
        byte[] decodedKey = Base64.getDecoder().decode(secret);
        byte[] key = new byte[32]; // AES-256은 32바이트 키 필요
        System.arraycopy(decodedKey, 0, key, 0, Math.min(decodedKey.length, 32));
        this.secretKey = new SecretKeySpec(key, "AES");
        this.secureRandom = new SecureRandom();
    }

    /**
     * 문자열 암호화
     * @param plainText 암호화할 평문
     * @return Base64 인코딩된 암호문 (IV + 암호화된 데이터)
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            // IV 생성 (12바이트)
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Cipher 초기화
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // 암호화
            byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV + 암호화된 데이터를 합침
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedData.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedData);

            // Base64 인코딩하여 반환
            return Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            log.error("암호화 실패", e);
            throw new RuntimeException("암호화 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 문자열 복호화
     * @param encryptedText Base64 인코딩된 암호문
     * @return 복호화된 평문
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            // Base64 디코딩
            byte[] decodedData = Base64.getDecoder().decode(encryptedText);

            // IV와 암호화된 데이터 분리
            ByteBuffer byteBuffer = ByteBuffer.wrap(decodedData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] encryptedData = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedData);

            // Cipher 초기화
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // 복호화
            byte[] decryptedData = cipher.doFinal(encryptedData);
            return new String(decryptedData, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("복호화 실패", e);
            throw new RuntimeException("복호화 중 오류가 발생했습니다.", e);
        }
    }
}
