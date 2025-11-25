package dev.woori.wooriBank.domain.account.service;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.domain.account.dto.AccountLookupReqDto;
import dev.woori.wooriBank.domain.account.dto.AccountLookupResDto;
import dev.woori.wooriBank.domain.account.dto.request.AccountCreateReqDto;
import dev.woori.wooriBank.domain.account.dto.request.AccountFormReqDto;
import dev.woori.wooriBank.domain.account.dto.request.TermsSubmitReqDto;
import dev.woori.wooriBank.domain.account.dto.response.UserAccountResDto;
import dev.woori.wooriBank.domain.auth.entity.AuthSession;
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
 * 4. POST /api/account/lookup - 계좌 조회
 * Redis 세션을 통해 단계별 정보를 저장하고 최종적으로 계좌를 개설
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AuthStoreRedis redis;
    private final UserAccountService userAccountService;
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter BASIC_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 1단계: 약관 동의 처리
     *
     * @param userId  사용자 ID (JWT에서 추출)
     * @param request 약관 동의 정보
     */
    public void submitTerms(String userId, TermsSubmitReqDto request) {
        AuthSession session = getSessionOrThrow(userId);
        session.validateVerified();

        // 약관 동의 상태 업데이트
        session.setTermsAgreed(request.termsAgreed());
        redis.save(userId, session);

        log.debug("약관 동의 완료 - userId: {}", userId);
    }

    /**
     * 2단계: 추가 정보 입력 처리
     *
     * @param userId  사용자 ID (JWT에서 추출)
     * @param request 추가 정보 (이메일, 영문 이름, 초기 입금액)
     */
    public void submitAccountForm(String userId, AccountFormReqDto request) {
        AuthSession session = getSessionOrThrow(userId);
        session.validateVerified();
        session.validateTermsAgreed();

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
     * @param userId  사용자 ID (JWT에서 추출)
     * @param request 계좌 PIN
     * @return 생성된 계좌 정보
     */
    public UserAccountResDto createAccount(String userId, AccountCreateReqDto request) {
        AuthSession session = getSessionOrThrow(userId);
        session.validateVerified();
        session.validateTermsAgreed();
        session.validateAccountFormCompleted();

        // 4. Redis 세션 정보를 CreateUserAccountReqDto로 변환
        LocalDate birthDate = parseBirth(session.getBirth());

        CreateUserAccountReqDto createReq = new CreateUserAccountReqDto(
                session.getName(), // nameKr
                session.getNameEn(), // nameEn
                session.getEmail(), // email
                session.getPhone(), // phoneNumber
                birthDate, // birth
                request.accountPin(), // accountPin
                session.getInitialBalance() // initialBalance
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
     * 계좌 조회
     *
     * @param request 계좌 조회 요청 (id, code)
     * @return 계좌 조회 결과 (이름, 계좌번호)
     */
    public AccountLookupResDto accountLookup(AccountLookupReqDto request) {
        String id = request.id();
        String code = request.code();

        // 코드와 redis 내부 저장소에 저장된 코드를 비교하기
        AuthSession session = redis.get(id);

        // 시간만료 등의 이유로 데이터가 사라진 경우
        if (session == null) {
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND, "데이터를 찾을 수 없습니다.");
        }
        // 코드 검증이 실패할 경우
        if (!code.equals(session.getCode())) {
            throw new CommonException(ErrorCode.UNAUTHORIZED, "인증 코드 오류");
        }

        // 검증 성공 후 정보 삭제
        redis.delete(id);

        // 이름 & 계좌번호 return
        return new AccountLookupResDto(session.getName(), session.getAccountNum());
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
     * 생년월일 문자열(YYYYMMDD 또는 YYYY-MM-DD)을 LocalDate로 변환
     *
     * @param birth 생년월일 문자열 (YYYYMMDD 또는 YYYY-MM-DD)
     * @return LocalDate 객체
     * @throws CommonException birth가 null이거나 빈 문자열일 때, 또는 형식이 올바르지 않을 때
     */
    private LocalDate parseBirth(String birth) {
        // null 또는 빈 문자열 검증 (Java 11+)
        if (birth == null || birth.isBlank()) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "생년월일은 필수입니다.");
        }

        String trimmedBirth = birth.trim();

        try {
            // '-' 포함 여부로 형식 구분하여 DateTimeFormatter 선택
            DateTimeFormatter formatter = trimmedBirth.contains("-")
                    ? ISO_DATE_FORMATTER
                    : BASIC_DATE_FORMATTER;

            return LocalDate.parse(trimmedBirth, formatter);
        } catch (DateTimeParseException e) {
            throw new CommonException(ErrorCode.INVALID_REQUEST,
                    "생년월일 형식이 올바르지 않습니다. (지원 형식: YYYYMMDD 또는 YYYY-MM-DD)");
        }
    }
}
