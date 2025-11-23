package dev.woori.wooriBank.domain.auth.dto;

import lombok.*;

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
    private boolean verified; // 검증 여부

    // 이후 필요한 필드 구현하면서 추가하기
}
