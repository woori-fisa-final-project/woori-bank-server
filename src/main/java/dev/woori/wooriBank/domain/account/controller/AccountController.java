package dev.woori.wooriBank.domain.account.controller;

import dev.woori.wooriBank.config.response.ApiResponse;
import dev.woori.wooriBank.config.response.BaseResponse;
import dev.woori.wooriBank.config.response.SuccessCode;
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

    @PostMapping("/lookup")
    public ResponseEntity<BaseResponse<?>> accountLookup(@Valid @RequestBody AccountLookupReqDto request){
        return ApiResponse.success(SuccessCode.OK, accountService.accountLookup(request));
    }
}
