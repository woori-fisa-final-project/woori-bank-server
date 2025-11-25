package dev.woori.wooriBank.domain.auth.controller;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.config.response.ApiResponse;
import dev.woori.wooriBank.config.response.BaseResponse;
import dev.woori.wooriBank.config.response.SuccessCode;
import dev.woori.wooriBank.domain.auth.dto.AuthReqDto;
import dev.woori.wooriBank.domain.auth.dto.AuthVerifyReqDto;
import dev.woori.wooriBank.domain.auth.dto.RefreshReqDto;
import dev.woori.wooriBank.domain.auth.entity.BankClientApp;
import dev.woori.wooriBank.domain.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * JWT 토큰 발급
     * AppKeySecretFilter에서 인증된 클라이언트 정보로 토큰 발급
     *
     * @param request HttpServletRequest (AppKeySecretFilter에서 clientApp attribute 설정)
     * @return JWT access token + refresh token
     */
    @PostMapping("/token")
    public ResponseEntity<BaseResponse<?>> issueToken(HttpServletRequest request) {
        // AppKeySecretFilter에서 설정한 clientApp 가져오기
        Object clientAppObj = request.getAttribute("clientApp");

        // instanceof 패턴 매칭으로 타입 체크와 캐스팅을 동시에 처리
        if (clientAppObj instanceof BankClientApp clientApp) {
            // 인증된 클라이언트의 이름으로 토큰 발급
            return ApiResponse.success(SuccessCode.OK, authService.issueToken(clientApp.getName()));
        } else if (clientAppObj == null) {
            throw new CommonException(ErrorCode.UNAUTHORIZED, "인증된 클라이언트 정보를 찾을 수 없습니다.");
        } else {
            throw new CommonException(ErrorCode.UNAUTHORIZED, "올바르지 않은 클라이언트 정보 형식입니다.");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<BaseResponse<?>> refresh(@RequestBody RefreshReqDto refreshToken) {
        return ApiResponse.success(SuccessCode.OK, authService.refresh(refreshToken));
    }

    // 본인인증 정보를 입력하면 인증번호를 발송해준다고 가정
    // 서버에서는 ok 응답만 보내줌
    @PostMapping("/request")
    public ResponseEntity<BaseResponse<?>> request(@AuthenticationPrincipal String username,
                                                   @RequestBody AuthReqDto authReqDto){
        authService.request(username, authReqDto);
        return ApiResponse.success(SuccessCode.OK);
    }

    @PostMapping("/verify")
    public ResponseEntity<BaseResponse<?>> verify(@AuthenticationPrincipal String username,
                                                  @RequestBody AuthVerifyReqDto request){
        authService.verify(username, request);
        return ApiResponse.success(SuccessCode.OK);
    }
}
