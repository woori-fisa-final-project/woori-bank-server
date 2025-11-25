package dev.woori.wooriBank.domain.account.service;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.domain.account.dto.request.AccountCreateReqDto;
import dev.woori.wooriBank.domain.account.dto.request.AccountFormReqDto;
import dev.woori.wooriBank.domain.account.dto.request.TermsSubmitReqDto;
import dev.woori.wooriBank.domain.account.dto.response.UserAccountResDto;
import dev.woori.wooriBank.domain.auth.dto.AuthSession;
import dev.woori.wooriBank.domain.auth.entity.AuthStoreRedis;
import dev.woori.wooriBank.domain.users.dto.CreateUserAccountReqDto;
import dev.woori.wooriBank.domain.users.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 계좌 개설 프로세스 서비스 (3단계)
 * 1. POST /api/terms/submit - 약관 동의
 * 2. POST /api/account/form - 추가 정보 입력
 * 3. POST /api/account/create - 계좌 개설
 * Redis 세션을 통해 단계별 정보를 저장하고 최종적으로 계좌를 개설
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AuthStoreRedis redis;
    private final UserAccountService userAccountService;

    /**
     * 1단계: 약관 동의 처리
     *
     * @param userId 사용자 ID (JWT에서 추출)
     * @param request 약관 동의 정보
     */
    public void submitTerms(String userId, TermsSubmitReqDto request) {
        AuthSession session = getSessionOrThrow(userId);
        validateVerified(session);

        // 약관 동의 상태 업데이트
        session.setTermsAgreed(request.termsAgreed());
        redis.save(userId, session);

        log.debug("약관 동의 완료 - userId: {}", userId);
    }

    /**
     * 2단계: 추가 정보 입력 처리
     *
     * @param userId 사용자 ID (JWT에서 추출)
     * @param request 추가 정보 (이메일, 영문 이름, 초기 입금액)
     */
    public void submitAccountForm(String userId, AccountFormReqDto request) {
        AuthSession session = getSessionOrThrow(userId);
        validateVerified(session);
        validateTermsAgreed(session);

        // 추가 정보 업데이트
        session.setEmail(request.email());
        session.setNameEn(request.nameEn());
        session.setInitialBalance(request.initialBalance());
        redis.save(userId, session);

        log.debug("추가 정보 입력 완료 - userId: {}, email: {}", userId, request.email());
    }

    /**
     * 3단계: 계좌 개설 (최종 단계)
     *
     * @param userId 사용자 ID (JWT에서 추출)
     * @param request 계좌 PIN
     * @return 생성된 계좌 정보
     */
    public UserAccountResDto createAccount(String userId, AccountCreateReqDto request) {
        AuthSession session = getSessionOrThrow(userId);
        validateVerified(session);
        validateTermsAgreed(session);
        validateAccountFormCompleted(session);

        // 4. Redis 세션 정보를 CreateUserAccountReqDto로 변환
        LocalDate birthDate = parseBirth(session.getBirth());

        CreateUserAccountReqDto createReq = new CreateUserAccountReqDto(
                session.getName(),           // nameKr
                session.getNameEn(),         // nameEn
                session.getEmail(),          // email
                session.getPhone(),          // phoneNumber
                birthDate,                   // birth
                request.accountPin(),        // accountPin
                session.getInitialBalance()  // initialBalance
        );

        // 5. 계좌 개설 (UserAccountService 호출)
        UserAccountResDto result = userAccountService.createUserWithAccount(userId, createReq);

        // 6. 계좌 개설 완료 후 Redis 세션 삭제
        redis.delete(userId);

        log.info("계좌 개설 완료 - userId: {}, accountId: {}, accountNumber: {}",
                userId, result.accountId(), result.accountNumber());

        return result;
    }

    /**
     * Redis에서 세션을 가져오고, 없으면 예외 발생
     */
    private AuthSession getSessionOrThrow(String userId) {
        AuthSession session = redis.get(userId);
        if (session == null) {
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                    "세션이 만료되었습니다. 처음부터 다시 시작해주세요.");
        }
        return session;
    }

    /**
     * 본인인증 완료 여부 검증
     */
    private void validateVerified(AuthSession session) {
        if (!session.isVerified()) {
            throw new CommonException(ErrorCode.FORBIDDEN, "본인인증을 먼저 완료해야 합니다.");
        }
    }

    /**
     * 약관 동의 완료 여부 검증
     */
    private void validateTermsAgreed(AuthSession session) {
        if (session.getTermsAgreed() == null || !session.getTermsAgreed()) {
            throw new CommonException(ErrorCode.FORBIDDEN, "약관 동의를 먼저 완료해야 합니다.");
        }
    }

    /**
     * 추가 정보 입력 완료 여부 검증
     */
    private void validateAccountFormCompleted(AuthSession session) {
        if (session.getEmail() == null || session.getInitialBalance() == null) {
            throw new CommonException(ErrorCode.FORBIDDEN, "추가 정보를 먼저 입력해야 합니다.");
        }
    }

    /**
     * 생년월일 문자열(YYYYMMDD 또는 YYYY-MM-DD)을 LocalDate로 변환
     *
     * @param birth 생년월일 문자열 (YYYYMMDD 또는 YYYY-MM-DD)
     * @return LocalDate 객체
     * @throws CommonException 생년월일 형식이 올바르지 않을 때
     */
    private LocalDate parseBirth(String birth) {
        try {
            if (birth.length() == 8) {
                // YYYYMMDD 형식
                return LocalDate.parse(birth, DateTimeFormatter.ofPattern("yyyyMMdd"));
            } else {
                // YYYY-MM-DD 형식
                return LocalDate.parse(birth, DateTimeFormatter.ISO_LOCAL_DATE);
            }
        } catch (DateTimeParseException e) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "생년월일 형식이 올바르지 않습니다.");
        }
    }
}
