package dev.woori.wooriBank.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 주민등록번호(RRN) 입력 요청 DTO
 */
public record RrnReqDto(
        @NotBlank(message = "TID는 필수입니다") String tid,

        @NotBlank(message = "주민등록번호는 필수입니다") @Pattern(regexp = "^\\d{6}-[1-4]\\d{6}$", message = "주민등록번호 형식이 올바르지 않습니다 (예: 990101-1234567)") String rrn) {
}
