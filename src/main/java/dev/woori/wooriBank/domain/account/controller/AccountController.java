package dev.woori.wooriBank.domain.account.controller;

import dev.woori.wooriBank.config.response.ApiResponse;
import dev.woori.wooriBank.config.response.BaseResponse;
import dev.woori.wooriBank.config.response.SuccessCode;
import dev.woori.wooriBank.domain.account.dto.AccountCreateReqDto;
import dev.woori.wooriBank.domain.account.dto.AccountFormReqDto;
import dev.woori.wooriBank.domain.account.dto.AccountLookupReqDto;
import dev.woori.wooriBank.domain.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/tid")
    public ResponseEntity<BaseResponse<?>> getTid(@RequestAttribute("clientApp") String clientId){
        return ApiResponse.success(SuccessCode.OK, accountService.getTid(clientId));
    }

    /**
     * 추가 정보 입력 (소속 기관, 이메일)
     */
    @PostMapping("/form")
    public ResponseEntity<BaseResponse<?>> saveAdditionalInfo(@Valid @RequestBody AccountFormReqDto request){
        return ApiResponse.success(SuccessCode.OK, accountService.saveAdditionalInfo(request));
    }

    /**
     * 계좌 개설 (실제 DB 저장, Code 발급 및 Redirect)
     */
    @PostMapping("/create")
    public ResponseEntity<BaseResponse<?>> createAccount(@Valid @RequestBody AccountCreateReqDto request){
        return ApiResponse.success(SuccessCode.CREATED, accountService.createAccount(request));
    }

    /**
     * 계좌 조회 (Code 기반)
     */
    @PostMapping("/lookup")
    public ResponseEntity<BaseResponse<?>> accountLookup(@Valid @RequestBody AccountLookupReqDto request){
        return ApiResponse.success(SuccessCode.OK, accountService.accountLookup(request));
    }
}
