package dev.woori.wooriBank.domain.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 주민등록번호(RRN) 유효성 검증기
 *
 * 체크섬 계산 알고리즘:
 * 1. 앞 12자리 각 자리에 가중치를 곱함: [2,3,4,5,6,7,8,9,2,3,4,5]
 * 2. 곱한 값을 모두 더함
 * 3. 합계를 11로 나눈 나머지를 구함
 * 4. 11에서 나머지를 뺌
 * 5. 그 값을 10으로 나눈 나머지가 체크섬 (마지막 13번째 자리)
 *
 * 예시: 990101-1234567
 * - 앞 12자리: 9,9,0,1,0,1,1,2,3,4,5,6
 * - 가중치:    2,3,4,5,6,7,8,9,2,3,4,5
 * - 곱셈:     18,27,0,5,0,7,8,18,6,12,20,30 = 151
 * - 151 % 11 = 8
 * - 11 - 8 = 3
 * - 3 % 10 = 3
 * - 체크섬이 3이면 유효 (실제 마지막 자리와 비교)
 */
public class RrnValidator implements ConstraintValidator<ValidRrn, String> {

    private static final String RRN_PATTERN = "^\\d{6}-[1-4]\\d{6}$";
    private static final int[] WEIGHTS = {2, 3, 4, 5, 6, 7, 8, 9, 2, 3, 4, 5};

    @Override
    public boolean isValid(String rrn, ConstraintValidatorContext context) {
        // null은 @NotBlank가 처리하므로 여기서는 통과
        if (rrn == null) {
            return true;
        }

        // 1. 형식 검증 (정규식)
        if (!rrn.matches(RRN_PATTERN)) {
            return false;
        }

        // 2. 체크섬 검증
        return validateChecksum(rrn);
    }

    /**
     * 주민등록번호 체크섬 검증
     * @param rrn 주민등록번호 (######-#######)
     * @return 체크섬이 유효하면 true
     */
    private boolean validateChecksum(String rrn) {
        // 하이픈 제거
        String digits = rrn.replace("-", "");

        // 앞 12자리 추출
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(digits.charAt(i));
            sum += digit * WEIGHTS[i];
        }

        // 체크섬 계산
        int remainder = sum % 11;
        int checksum = (11 - remainder) % 10;

        // 실제 마지막 자리와 비교
        int lastDigit = Character.getNumericValue(digits.charAt(12));
        return checksum == lastDigit;
    }
}
