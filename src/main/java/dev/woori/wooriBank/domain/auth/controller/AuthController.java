package dev.woori.wooriBank.domain.auth.controller;


import dev.woori.wooriBank.config.response.ApiResponse;
import dev.woori.wooriBank.config.response.BaseResponse;
import dev.woori.wooriBank.config.response.SuccessCode;
import dev.woori.wooriBank.domain.auth.dto.LoginReqDto;
import dev.woori.wooriBank.domain.auth.dto.RefreshReqDto;
import dev.woori.wooriBank.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<?>> login(@RequestBody LoginReqDto loginReqDto) {
        return ApiResponse.success(SuccessCode.OK, authService.login(loginReqDto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<BaseResponse<?>> refresh(@RequestBody RefreshReqDto refreshToken) {
        return ApiResponse.success(SuccessCode.OK, authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<?>> logout(Principal principal) {
        return ApiResponse.success(SuccessCode.OK, authService.logout(principal.getName()));
    }
}
