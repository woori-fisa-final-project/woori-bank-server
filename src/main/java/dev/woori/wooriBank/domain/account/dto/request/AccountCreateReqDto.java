package dev.woori.wooriBank.domain.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 계좌 개설 요청 DTO
 *
 * ⚠️ 보안 주의사항:
 * - 현재 4자리 PIN은 10,000개 경우의 수로 무차별 대입 공격에 취약함
 * - BCrypt 해싱으로 저장하지만, 레인보우 테이블 공격 가능성 존재
 * - 프로덕션 환경에서는 반드시 다음 보안 조치 필요:
 *   1. 계좌 잠금 (5회 실패 시 30분 잠금 등)
 *   2. Rate Limiting (IP/계좌당 시도 횟수 제한)
 *   3. 비밀번호 변경 주기 정책
 *
 * ⚠️ redirectUrl 보안:
 * - Open Redirect 취약점 방지를 위해 클라이언트 요청값 사용 안함
 * - DB에 미리 등록된 redirectUrl만 사용 (BankClientApp 테이블)
 *
 * TODO: 비즈니스 요구사항 재검토 및 보안 강화
 *       (예: 최소 6자리 이상, 영문+숫자 조합 등)
 */
public record AccountCreateReqDto(
        @NotBlank(message = "TID는 필수입니다")
        String tid,

        @NotBlank(message = "비밀번호는 필수입니다")
        @Pattern(regexp = "^\\d{4}$", message = "비밀번호는 4자리 숫자여야 합니다")
        String password // TODO: 보안 강화 필요 (현재 4자리 PIN은 취약)
) {
}
