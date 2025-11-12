package dev.woori.wooriBank.domain.auth.controller;

import dev.woori.wooriBank.config.response.ApiResponse;
import dev.woori.wooriBank.config.response.BaseResponse;
import dev.woori.wooriBank.config.response.SuccessCode;
import dev.woori.wooriBank.domain.auth.dto.RefreshReqDto;
import dev.woori.wooriBank.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/token")
    public ResponseEntity<BaseResponse<?>> issueToken(@RequestHeader("appKey") String appKey,
                                               @RequestHeader("secretKey") String secretKey){
        return ApiResponse.success(SuccessCode.OK, authService.issueToken(appKey, secretKey));
    }

    @PostMapping("/refresh")
    public ResponseEntity<BaseResponse<?>> refresh(@RequestBody RefreshReqDto refreshToken) {
        return ApiResponse.success(SuccessCode.OK, authService.refresh(refreshToken));
    }
}
