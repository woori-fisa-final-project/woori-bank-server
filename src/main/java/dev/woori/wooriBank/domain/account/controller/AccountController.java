package dev.woori.wooriBank.domain.account.controller;

import dev.woori.wooriBank.config.response.ApiResponse;
import dev.woori.wooriBank.config.response.BaseResponse;
import dev.woori.wooriBank.config.response.SuccessCode;
import dev.woori.wooriBank.domain.account.dto.AccountLookupReqDto;
import dev.woori.wooriBank.domain.account.dto.request.AccountCreateReqDto;
import dev.woori.wooriBank.domain.account.dto.request.AccountFormReqDto;
import dev.woori.wooriBank.domain.account.dto.request.TermsSubmitReqDto;
import dev.woori.wooriBank.domain.account.dto.response.UserAccountResDto;
import dev.woori.wooriBank.domain.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 계좌 개설 프로세스 컨트롤러
 * 1. POST /api/account/terms - 약관 동의
 * 2. POST /api/account/form - 추가 정보 입력
 * 3. POST /api/account/create - 계좌 개설
 * 4. POST /api/account/lookup - 계좌 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * 1단계: 약관 동의
     *
     * POST /api/account/terms
     *
     * @param username JWT에서 추출한 사용자 ID (userId)
     * @param request  약관 동의 정보
     * @return 성공 응답
     */
    @PostMapping("/terms")
    public ResponseEntity<BaseResponse<?>> submitTerms(
            @AuthenticationPrincipal String username,
            @Valid @RequestBody TermsSubmitReqDto request) {
        accountService.submitTerms(username, request);
        return ApiResponse.success(SuccessCode.OK);
    }

    /**
     * 2단계: 추가 정보 입력
     *
     * POST /api/account/form
     *
     * @param username JWT에서 추출한 사용자 ID (userId)
     * @param request  추가 정보 (이메일, 영문 이름, 초기 입금액)
     * @return 성공 응답
     */
    @PostMapping("/form")
    public ResponseEntity<BaseResponse<?>> submitAccountForm(
            @AuthenticationPrincipal String username,
            @Valid @RequestBody AccountFormReqDto request) {
        accountService.submitAccountForm(username, request);
        return ApiResponse.success(SuccessCode.OK);
    }

    /**
     * 3단계: 계좌 개설 (최종)
     *
     * POST /api/account/create
     *
     * @param username JWT에서 추출한 사용자 ID (userId)
     * @param request  계좌 PIN (4자리 숫자)
     * @return 생성된 계좌 정보
     */
    @PostMapping("/create")
    public ResponseEntity<BaseResponse<?>> createAccount(
            @AuthenticationPrincipal String username,
            @Valid @RequestBody AccountCreateReqDto request) {
        UserAccountResDto result = accountService.createAccount(username, request);
        return ApiResponse.success(SuccessCode.CREATED, result);
    }

    /**
     * 계좌 조회
     *
     * POST /api/account/lookup
     *
     * @param request 계좌 조회 요청 정보
     * @return 계좌 조회 결과
     */
    @PostMapping("/lookup")
    public ResponseEntity<BaseResponse<?>> accountLookup(@Valid @RequestBody AccountLookupReqDto request) {
        return ApiResponse.success(SuccessCode.OK, accountService.accountLookup(request));
    }
}
