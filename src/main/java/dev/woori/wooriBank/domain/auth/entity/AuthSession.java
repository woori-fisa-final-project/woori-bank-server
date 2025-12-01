package dev.woori.wooriBank.domain.auth.entity;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthSession{
    private String tid;
    private String clientId;

    // 본인인증 정보
    private String name; // 사용자 이름
    private String phone; // 사용자 전화번호
    private String birth; // 사용자 생년월일
    private String rrn; // 주민등록번호 (Resident Registration Number, 예: 990101-1234567)

    private String authCode; // 인증번호
    private int failedAttempts; // 인증번호 실패 횟수
    @Builder.Default
    private int resendAttempts = 0; // 인증번호 재발송 횟수
    private boolean verified; // 검증 여부

    // 약관 동의 정보
    private List<TermAgreement> terms; // 약관 동의 목록
    private boolean termsAgreed; // 필수 약관 동의 완료 여부

    // 추가 정보
    private String engName; // 영어 이름
    private String email; // 이메일

    // 계좌 정보
    private String accountNumber; // 계좌번호
    private String code; // 생성 완료 후 외부 서비스가 접근 가능하도록 해주는 코드(일회용)

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TermAgreement {
        private String termId;   // 약관 ID (PERSONAL_INFO, SERVICE_TERMS, MARKETING)
        private boolean agreed;  // 동의 여부
    }
}
