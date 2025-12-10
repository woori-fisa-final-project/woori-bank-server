package dev.woori.wooriBank.domain.auth.dto;

import dev.woori.wooriBank.domain.validation.ValidRrn;
import jakarta.validation.constraints.NotBlank;

/**
 * 주민등록번호(RRN) 입력 요청 DTO
 *
 * @ValidRrn 애노테이션이 다음을 자동으로 검증:
 * - 형식 검증 (######-#######)
 * - 성별코드 검증 (1~4)
 * - 체크섬 검증 (마지막 자리)
 */
public record RrnReqDto(
        @NotBlank(message = "TID는 필수입니다")
        String tid,

        @NotBlank(message = "주민등록번호는 필수입니다")
        @ValidRrn
        String rrn
) {
}
