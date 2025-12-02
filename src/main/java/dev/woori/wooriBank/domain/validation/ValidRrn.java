package dev.woori.wooriBank.domain.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 주민등록번호(RRN) 유효성 검증 애노테이션
 *
 * 검증 항목:
 * 1. 형식 검증: ######-#######
 * 2. 자릿수 검증: 총 13자리 (앞 6자리 + 뒤 7자리)
 * 3. 성별코드 검증: 1~4 범위
 * 4. 체크섬 검증: 마지막 자리는 앞 12자리로부터 계산된 체크섬
 */
@Documented
@Constraint(validatedBy = RrnValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRrn {
    String message() default "주민등록번호가 유효하지 않습니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
