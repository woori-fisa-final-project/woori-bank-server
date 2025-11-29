package dev.woori.wooriBank.domain.terms.service;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.domain.auth.entity.AuthSession;
import dev.woori.wooriBank.domain.auth.entity.AuthStoreRedis;
import dev.woori.wooriBank.domain.terms.dto.TermsSubmitReqDto;
import dev.woori.wooriBank.domain.terms.dto.TermsSubmitResDto;
import dev.woori.wooriBank.domain.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 약관 동의 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TermsService {

    private final AuthStoreRedis redis;
    private final ValidationUtil validationUtil;

    private static final List<String> VALID_TERM_IDS = List.of(
            "PERSONAL_INFO",    // 개인정보 수집 및 이용 동의
            "SERVICE_TERMS",    // 서비스 이용약관 동의
            "MARKETING"         // 마케팅 정보 수신 동의 (선택)
    );

    private static final List<String> REQUIRED_TERM_IDS = List.of(
            "PERSONAL_INFO",
            "SERVICE_TERMS"
    );

    /**
     * 약관 동의 처리
     */
    public TermsSubmitResDto submitTerms(TermsSubmitReqDto request) {
        // 1. TID 세션 조회
        AuthSession session = validationUtil.getSessionOrThrow(request.tid());

        // 2. 본인인증 완료 확인
        if (!session.isVerified()) {
            throw new CommonException(ErrorCode.FORBIDDEN, "본인인증을 먼저 완료해주세요");
        }

        // 3. 약관 ID 유효성 검증
        validateTermIds(request.terms());

        // 4. 필수 약관 동의 확인
        validateRequiredTerms(request.terms());

        // 5. 세션에 약관 정보 저장
        List<AuthSession.TermAgreement> termAgreements = request.terms().stream()
                .map(term -> AuthSession.TermAgreement.builder()
                        .termId(term.termId())
                        .agreed(term.agreed())
                        .build())
                .collect(Collectors.toList());

        session.setTerms(termAgreements);
        session.setTermsAgreed(true);

        // 6. Redis 저장
        redis.save(request.tid(), session);

        log.info("[약관 동의 완료] TID: {}, 필수약관: {}, 선택약관: {}",
                request.tid(),
                countAgreedTerms(request.terms(), REQUIRED_TERM_IDS),
                countAgreedTerms(request.terms(), List.of("MARKETING")));

        return new TermsSubmitResDto(true);
    }

    /**
     * 약관 ID 유효성 검증
     */
    private void validateTermIds(List<TermsSubmitReqDto.TermAgreement> terms) {
        for (TermsSubmitReqDto.TermAgreement term : terms) {
            if (!VALID_TERM_IDS.contains(term.termId())) {
                throw new CommonException(ErrorCode.INVALID_REQUEST,
                        "유효하지 않은 약관 ID입니다: " + term.termId());
            }
        }
    }

    /**
     * 필수 약관 동의 확인
     * 동의한 약관 ID들을 Set으로 추출한 후, 필수 약관이 모두 포함되어 있는지 확인
     */
    private void validateRequiredTerms(List<TermsSubmitReqDto.TermAgreement> terms) {
        // 동의한 약관 ID들을 Set으로 추출
        Set<String> agreedTermIds = terms.stream()
                .filter(term -> Boolean.TRUE.equals(term.agreed()))
                .map(TermsSubmitReqDto.TermAgreement::termId)
                .collect(Collectors.toSet());

        // 필수 약관 중 동의하지 않은 항목 찾기
        List<String> missingTerms = REQUIRED_TERM_IDS.stream()
                .filter(requiredTermId -> !agreedTermIds.contains(requiredTermId))
                .collect(Collectors.toList());

        if (!missingTerms.isEmpty()) {
            throw new CommonException(ErrorCode.INVALID_REQUEST,
                    "필수 약관에 동의해주세요: " + String.join(", ", missingTerms));
        }
    }

    /**
     * 동의한 약관 개수 카운트
     */
    private long countAgreedTerms(List<TermsSubmitReqDto.TermAgreement> terms, List<String> termIds) {
        return terms.stream()
                .filter(term -> termIds.contains(term.termId()) && Boolean.TRUE.equals(term.agreed()))
                .count();
    }
}
