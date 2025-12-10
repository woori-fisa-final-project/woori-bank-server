package dev.woori.wooriBank.domain.util;

import org.springframework.stereotype.Component;

/**
 * 개인정보 마스킹 유틸리티
 * 로그 출력 시 민감한 정보를 마스킹하여 개인정보 보호
 */
@Component
public class MaskingUtil {

    /**
     * 전화번호 마스킹
     * 예: 01012345678 -> 010****5678
     */
    public String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * Code 마스킹 (길이 유지)
     * 예: Xy7a1234abcd5678 -> Xy7a********5678
     */
    public String maskCode(String code) {
        if (code == null || code.length() < 8) {
            return code;
        }
        int visibleChars = 4; // 앞뒤로 보이는 글자 수
        int maskedLength = code.length() - (visibleChars * 2);
        String masked = "*".repeat(Math.max(0, maskedLength));
        return code.substring(0, visibleChars) + masked + code.substring(code.length() - visibleChars);
    }

    /**
     * 이메일 마스킹
     * 예: user@example.com -> u***@example.com
     */
    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        if (localPart.length() <= 1) {
            return email;
        }
        return localPart.charAt(0) + "***@" + parts[1];
    }
}
