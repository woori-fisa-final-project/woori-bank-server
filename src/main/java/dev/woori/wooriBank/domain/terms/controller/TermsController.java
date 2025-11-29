package dev.woori.wooriBank.domain.terms.controller;

import dev.woori.wooriBank.config.response.ApiResponse;
import dev.woori.wooriBank.config.response.BaseResponse;
import dev.woori.wooriBank.config.response.SuccessCode;
import dev.woori.wooriBank.domain.terms.dto.TermsSubmitReqDto;
import dev.woori.wooriBank.domain.terms.dto.TermsSubmitResDto;
import dev.woori.wooriBank.domain.terms.service.TermsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 약관 동의 컨트롤러
 */
@RestController
@RequestMapping("/terms")
@RequiredArgsConstructor
public class TermsController {

    private final TermsService termsService;

    /**
     * 약관 동의 제출
     *
     * POST /terms/submit
     */
    @PostMapping("/submit")
    public ResponseEntity<BaseResponse<?>> submitTerms(@Valid @RequestBody TermsSubmitReqDto request) {
        TermsSubmitResDto response = termsService.submitTerms(request);
        return ApiResponse.success(SuccessCode.OK, response);
    }
}
