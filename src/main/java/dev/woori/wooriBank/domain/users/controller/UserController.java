package dev.woori.wooriBank.domain.users.controller;

import dev.woori.wooriBank.config.response.ApiResponse;
import dev.woori.wooriBank.config.response.BaseResponse;
import dev.woori.wooriBank.config.response.SuccessCode;
import dev.woori.wooriBank.domain.users.dto.CreateUserAccountReqDto;
import dev.woori.wooriBank.domain.users.dto.UserAccountResDto;
import dev.woori.wooriBank.domain.users.service.UserAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 회원 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserAccountService userAccountService;

    /**
     * 회원 생성 + 계좌 개설 (1인 1계좌)
     *
     * POST /api/users/{userId}/accounts
     *
     * @param userId 메인 서버의 userId (다른 백엔드 서버에서 관리하는 사용자 ID)
     * @param request 회원 정보 및 계좌 개설 정보
     * @return 생성된 회원 및 계좌 정보
     */
    @PostMapping("/{userId}/accounts")
    public ResponseEntity<BaseResponse<?>> createUserAccount(
            @PathVariable String userId,
            @Valid @RequestBody CreateUserAccountReqDto request
    ) {
        UserAccountResDto result = userAccountService.createUserWithAccount(userId, request);
        return ApiResponse.success(SuccessCode.CREATED, result);
    }
}
