package dev.woori.wooriBank.domain.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * MaskingUtil 테스트
 * 개인정보 마스킹 검증
 */
class MaskingUtilTest {

    private MaskingUtil maskingUtil;

    @BeforeEach
    void setUp() {
        maskingUtil = new MaskingUtil();
    }

    @Test
    @DisplayName("전화번호 마스킹 - 정상 케이스 (11자리)")
    void maskPhone_ShouldMaskCorrectly_WhenValidPhoneNumber() {
        // given
        String phone = "01012345678";

        // when
        String masked = maskingUtil.maskPhone(phone);

        // then
        assertThat(masked).isEqualTo("010****5678");
    }

    @Test
    @DisplayName("전화번호 마스킹 - null 입력 시 그대로 반환")
    void maskPhone_ShouldReturnNull_WhenNull() {
        // when
        String masked = maskingUtil.maskPhone(null);

        // then
        assertThat(masked).isNull();
    }

    @Test
    @DisplayName("전화번호 마스킹 - 11자리가 아닌 경우 그대로 반환")
    void maskPhone_ShouldReturnAsIs_WhenInvalidLength() {
        // given
        String shortPhone = "0101234567"; // 10자리
        String longPhone = "010123456789"; // 12자리

        // when
        String maskedShort = maskingUtil.maskPhone(shortPhone);
        String maskedLong = maskingUtil.maskPhone(longPhone);

        // then
        assertThat(maskedShort).isEqualTo(shortPhone);
        assertThat(maskedLong).isEqualTo(longPhone);
    }

    @Test
    @DisplayName("Code 마스킹 - 16자리 코드")
    void maskCode_ShouldMaskCorrectly_When16Characters() {
        // given
        String code = "Xy7a1234abcd5678"; // 16자리

        // when
        String masked = maskingUtil.maskCode(code);

        // then
        assertThat(masked).isEqualTo("Xy7a********5678");
        assertThat(masked).hasSize(code.length()); // 길이 유지
    }

    @Test
    @DisplayName("Code 마스킹 - 다양한 길이")
    void maskCode_ShouldMaskCorrectly_WithVariousLengths() {
        // given
        String code8 = "12345678"; // 8자리 (최소)
        String code12 = "123456789012"; // 12자리
        String code20 = "12345678901234567890"; // 20자리

        // when
        String masked8 = maskingUtil.maskCode(code8);
        String masked12 = maskingUtil.maskCode(code12);
        String masked20 = maskingUtil.maskCode(code20);

        // then
        assertThat(masked8).isEqualTo("12345678"); // 앞4 + 뒤4 = 8자리 (마스킹 없음)
        assertThat(masked12).isEqualTo("1234****9012"); // 앞4 + 마스킹4 + 뒤4
        assertThat(masked20).isEqualTo("1234************7890"); // 앞4 + 마스킹12 + 뒤4
    }

    @Test
    @DisplayName("Code 마스킹 - null 또는 8자리 미만은 그대로 반환")
    void maskCode_ShouldReturnAsIs_WhenNullOrTooShort() {
        // given
        String shortCode = "1234567"; // 7자리

        // when
        String maskedNull = maskingUtil.maskCode(null);
        String maskedShort = maskingUtil.maskCode(shortCode);

        // then
        assertThat(maskedNull).isNull();
        assertThat(maskedShort).isEqualTo(shortCode);
    }

    @Test
    @DisplayName("이메일 마스킹 - 정상 케이스")
    void maskEmail_ShouldMaskCorrectly_WhenValidEmail() {
        // given
        String email = "user@example.com";

        // when
        String masked = maskingUtil.maskEmail(email);

        // then
        assertThat(masked).isEqualTo("u***@example.com");
    }

    @Test
    @DisplayName("이메일 마스킹 - 다양한 케이스")
    void maskEmail_ShouldMaskCorrectly_WithVariousFormats() {
        // given
        String shortEmail = "a@test.com";
        String longEmail = "verylongemail@example.com";
        String multiDotEmail = "user.name@example.co.kr";

        // when
        String maskedShort = maskingUtil.maskEmail(shortEmail);
        String maskedLong = maskingUtil.maskEmail(longEmail);
        String maskedMultiDot = maskingUtil.maskEmail(multiDotEmail);

        // then
        assertThat(maskedShort).isEqualTo("a@test.com"); // 1글자는 그대로 (길이 <= 1)
        assertThat(maskedLong).isEqualTo("v***@example.com");
        assertThat(maskedMultiDot).isEqualTo("u***@example.co.kr");
    }

    @Test
    @DisplayName("이메일 마스킹 - null 또는 @가 없는 경우 그대로 반환")
    void maskEmail_ShouldReturnAsIs_WhenNullOrInvalid() {
        // given
        String invalidEmail = "notanemail";

        // when
        String maskedNull = maskingUtil.maskEmail(null);
        String maskedInvalid = maskingUtil.maskEmail(invalidEmail);

        // then
        assertThat(maskedNull).isNull();
        assertThat(maskedInvalid).isEqualTo(invalidEmail);
    }

    @Test
    @DisplayName("이메일 마스킹 - local part가 1글자인 경우 그대로 반환")
    void maskEmail_ShouldReturnAsIs_WhenLocalPartIsSingleChar() {
        // given
        String singleCharEmail = "a@test.com";

        // when
        String masked = maskingUtil.maskEmail(singleCharEmail);

        // then
        assertThat(masked).isEqualTo("a@test.com"); // 1글자는 마스킹하지 않음
    }

    @Test
    @DisplayName("실제 사용 케이스 - 로그 출력 시뮬레이션")
    void maskingUtil_RealUsageScenario() {
        // given
        String phone = "01012345678";
        String code = "AbCd1234XyZ56789";
        String email = "user@example.com";

        // when
        String logMessage = String.format(
                "[사용자 정보] Phone: %s, Code: %s, Email: %s",
                maskingUtil.maskPhone(phone),
                maskingUtil.maskCode(code),
                maskingUtil.maskEmail(email)
        );

        // then
        assertThat(logMessage).contains("010****5678");
        assertThat(logMessage).contains("AbCd********6789");
        assertThat(logMessage).contains("u***@example.com");
        assertThat(logMessage).doesNotContain("01012345678"); // 원본 전화번호 없음
        assertThat(logMessage).doesNotContain("user@example.com"); // 원본 이메일 없음
    }
}
