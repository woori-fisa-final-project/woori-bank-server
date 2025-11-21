package dev.woori.wooriBank.domain.account.controller;

import dev.woori.wooriBank.config.response.ApiResponse;
import dev.woori.wooriBank.config.response.BaseResponse;
import dev.woori.wooriBank.config.response.SuccessCode;
import dev.woori.wooriBank.domain.account.dto.request.AccountCreateDto;
import dev.woori.wooriBank.domain.account.dto.response.AccountResponse;
import dev.woori.wooriBank.domain.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 계좌 개설 컨트롤러
 * 명세서: POST /api/account/create
 */
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * 계좌 개설 (명세서 버전)
     *
     * POST /api/account/create
     *
     * @param externalUserId 메인 서버의 사용자 ID (Header에서 전달)
     * @param request 계좌 개설 요청 정보
     * @return 생성된 계좌 정보
     */
    @PostMapping("/create")
    public ResponseEntity<BaseResponse<?>> createAccount(
            @RequestHeader("X-User-Id") String externalUserId,
            @Valid @RequestBody AccountCreateDto request
    ) {
        AccountResponse response = accountService.createAccount(externalUserId, request);
        return ApiResponse.success(SuccessCode.CREATED, response);
    }
}
