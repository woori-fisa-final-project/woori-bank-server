package dev.woori.wooriBank.domain.auth.controller;

import dev.woori.wooriBank.config.response.ApiResponse;
import dev.woori.wooriBank.config.response.BaseResponse;
import dev.woori.wooriBank.config.response.SuccessCode;
import dev.woori.wooriBank.domain.auth.dto.AuthReqDto;
import dev.woori.wooriBank.domain.auth.dto.AuthVerifyReqDto;
import dev.woori.wooriBank.domain.auth.dto.RefreshReqDto;
import dev.woori.wooriBank.domain.auth.dto.TokenReqDto;
import dev.woori.wooriBank.domain.auth.service.AuthService;
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

    @PostMapping("/token")
    public ResponseEntity<BaseResponse<?>> issueToken(@RequestBody TokenReqDto request){
        return ApiResponse.success(SuccessCode.OK, authService.issueToken(request.userId()));
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
