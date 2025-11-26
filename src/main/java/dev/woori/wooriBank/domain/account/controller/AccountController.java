package dev.woori.wooriBank.domain.account.controller;

import dev.woori.wooriBank.config.response.ApiResponse;
import dev.woori.wooriBank.config.response.BaseResponse;
import dev.woori.wooriBank.config.response.SuccessCode;
import dev.woori.wooriBank.domain.account.dto.AccountLookupReqDto;
import dev.woori.wooriBank.domain.account.dto.TidReqDto;
import dev.woori.wooriBank.domain.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/tid")
    public ResponseEntity<BaseResponse<?>> getTid(@Valid @RequestBody TidReqDto request){
        return ApiResponse.success(SuccessCode.OK, accountService.getTid(request));
    }

    @PostMapping("/lookup")
    public ResponseEntity<BaseResponse<?>> accountLookup(@Valid @RequestBody AccountLookupReqDto request){
        return ApiResponse.success(SuccessCode.OK, accountService.accountLookup(request));
    }
}
