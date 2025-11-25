package dev.woori.wooriBank.domain.auth.entity;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthSession {
    private String id; // jwt 토큰값을 기반으로 한 id
    private String name; // 사용자 이름
    private String phone; // 사용자 전화번호
    private String birth; // 사용자 생년월일
    private String authCode; // 인증번호
    private int failedAttempts; // 인증번호 실패 횟수
    private boolean verified; // 본인인증 검증 여부

    // 계좌 개설 프로세스 추가 필드
    private Boolean termsAgreed; // 약관 동의 여부
    private String email; // 이메일 (추가 정보)
    private String nameEn; // 영문 이름 (추가 정보)
    private BigDecimal initialBalance; // 초기 입금액 (추가 정보)

    // 계좌 조회 관련 필드
    private String accountNum; // 계좌번호
    private String code; // 생성 완료 후 외부 서비스가 접근 가능하도록 해주는 코드(일회용)

    /**
     * 본인인증 완료 여부 검증
     * 
     * @throws CommonException 본인인증이 완료되지 않았을 때
     */
    public void validateVerified() {
        if (!this.verified) {
            throw new CommonException(ErrorCode.FORBIDDEN, "본인인증을 먼저 완료해야 합니다.");
        }
    }

    /**
     * 약관 동의 완료 여부 검증
     * 
     * @throws CommonException 약관 동의가 완료되지 않았을 때
     */
    public void validateTermsAgreed() {
        if (this.termsAgreed == null || !this.termsAgreed) {
            throw new CommonException(ErrorCode.FORBIDDEN, "약관 동의를 먼저 완료해야 합니다.");
        }
    }

    /**
     * 추가 정보 입력 완료 여부 검증
     * 
     * @throws CommonException 추가 정보가 입력되지 않았을 때
     */
    public void validateAccountFormCompleted() {
        if (this.email == null || this.initialBalance == null) {
            throw new CommonException(ErrorCode.FORBIDDEN, "추가 정보를 먼저 입력해야 합니다.");
        }
    }
}
