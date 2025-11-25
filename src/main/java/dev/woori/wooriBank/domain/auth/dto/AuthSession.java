package dev.woori.wooriBank.domain.auth.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthSession{
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
}
