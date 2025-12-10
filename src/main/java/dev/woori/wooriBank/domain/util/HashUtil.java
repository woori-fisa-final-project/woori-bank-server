package dev.woori.wooriBank.domain.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 해시 유틸리티 (SHA-256)
 * 암호화된 데이터 검색을 위한 해시 생성
 */
@Component
public class HashUtil {

    /**
     * SHA-256 해시 생성
     * @param input 평문 입력
     * @return Hex 인코딩된 해시값 (64자)
     */
    public String sha256(String input) {
        if (input == null) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다", e);
        }
    }

    /**
     * 바이트 배열을 Hex 문자열로 변환
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
