package dev.woori.wooriBank.domain.account.service;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.domain.account.dto.*;
import dev.woori.wooriBank.domain.account.entity.BankAccount;
import dev.woori.wooriBank.domain.account.repository.BankAccountRepository;
import dev.woori.wooriBank.domain.account.util.AccountNumberGenerator;
import dev.woori.wooriBank.domain.auth.entity.AuthSession;
import dev.woori.wooriBank.domain.auth.entity.AuthStoreRedis;
import dev.woori.wooriBank.domain.users.entity.BankUser;
import dev.woori.wooriBank.domain.users.repository.BankUserRepository;
import dev.woori.wooriBank.domain.util.MaskingUtil;
import dev.woori.wooriBank.domain.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AuthStoreRedis redis;
    private final ValidationUtil validationUtil;
    private final MaskingUtil maskingUtil;
    private final BankUserRepository bankUserRepository;
    private final BankAccountRepository bankAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountNumberGenerator accountNumberGenerator;

    // 날짜 형식 상수
    private static final String BIRTH_DATE_PATTERN = "yyyyMMdd";

    // 난수 생성 관련 상수
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 16;

    public TidResDto getTid(String clientId) {

        // 랜덤 tid 생성 및 저장
        String tid = UUID.randomUUID().toString();

        AuthSession session = AuthSession.builder()
                .tid(tid)
                .clientId(clientId)
                .build();

        // redis 저장
        redis.save(tid, session);

        return new TidResDto(tid);
    }

    /**
     * 추가 정보 입력 (소속 기관, 이메일)
     */
    public AccountFormResDto saveAdditionalInfo(AccountFormReqDto request) {
        // 1. TID 세션 조회
        AuthSession session = validationUtil.getSessionOrThrow(request.tid());

        // 2. 약관 동의 완료 확인
        if (!session.isTermsAgreed()) {
            throw new CommonException(ErrorCode.FORBIDDEN, "약관 동의를 먼저 완료해주세요");
        }

        // 3. 세션에 추가 정보 저장
        session.setOrgName(request.orgName());
        session.setEmail(request.email());

        // 4. Redis 저장
        redis.save(request.tid(), session);

        log.info("[추가 정보 저장 완료] TID: {}, 소속: {}, 이메일: {}",
                request.tid(), request.orgName(), maskingUtil.maskEmail(request.email()));

        return new AccountFormResDto(true);
    }

    /**
     * Code 기반 계좌 조회 (일회용)
     */
    public AccountLookupResDto accountLookup(AccountLookupReqDto request) {
        log.info("[계좌 조회 시작] Code: {}", maskingUtil.maskCode(request.code()));

        // 1. Code로 TID 조회
        String tid = redis.getTidByCode(request.code());
        if (tid == null) {
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND, "유효하지 않은 Code입니다");
        }

        // 2. TID로 세션 조회
        AuthSession session = validationUtil.getSessionOrThrow(tid);

        // 3. Code 검증 (세션에 저장된 Code와 일치하는지)
        if (!request.code().equals(session.getCode())) {
            throw new CommonException(ErrorCode.FORBIDDEN, "Code가 일치하지 않습니다");
        }

        // 4. 계좌번호로 실제 계좌 조회
        BankAccount account = bankAccountRepository.findByAccountNumber(session.getAccountNumber())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "계좌를 찾을 수 없습니다"));

        // 5. 일회용 Code 및 세션 삭제
        redis.deleteCode(request.code());
        redis.delete(tid);

        log.info("[계좌 조회 완료] UserId: {}, AccountNumber: {}",
                account.getUser().getId(), account.getAccountNumber());

        // 6. 완전한 계좌 정보 반환 (포인트 연동을 위한 userId 포함)
        return new AccountLookupResDto(
                account.getUser().getId(),
                session.getName(),
                account.getAccountNumber(),
                session.getEmail(),
                session.getOrgName()
        );
    }

    /**
     * 계좌 개설 (실제 DB 저장)
     * 트랜잭션 분리: DB 커밋 후 Redis 작업 수행하여 데이터 일관성 보장
     */
    @Transactional
    public AccountCreateResDto createAccount(AccountCreateReqDto request) {
        log.info("[계좌 개설 시작] TID: {}", request.tid());

        // 1. 세션 조회 및 검증
        AuthSession session = validateSessionForAccountCreation(request.tid());

        // 2. 사용자 고유성 검증 (전화번호, 이메일 중복 체크)
        validateUserUniqueness(session);

        // 3. Code 미리 생성 (중복 체크 포함)
        String code = generateCode();
        log.info("[Code 생성 완료] Code: {}, TID: {}", maskingUtil.maskCode(code), request.tid());

        try {
            // 4. BankUser 생성
            BankUser user = createUser(session);
            log.info("[사용자 생성 완료] UserId: {}, Phone: {}", user.getId(), maskingUtil.maskPhone(session.getPhone()));

            // 5. BankAccount 생성
            BankAccount account = createBankAccount(user, request.password());
            log.info("[계좌 생성 완료] AccountNumber: {}, UserId: {}", account.getAccountNumber(), user.getId());

            // 6. DB 트랜잭션 커밋 후 Redis 작업 수행
            String tid = request.tid();
            String accountNumber = account.getAccountNumber();

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        // 세션에 Code 및 계좌번호 저장
                        session.setCode(code);
                        session.setAccountNumber(accountNumber);

                        // Redis 저장 (세션 및 Code 매핑)
                        redis.save(tid, session);
                        redis.saveCode(code, tid, 600); // 10분 TTL (600초)

                        log.info("[Redis 저장 완료] TID: {}, Code: {}", tid, maskingUtil.maskCode(code));
                    } catch (Exception e) {
                        // Redis 저장 실패 시 에러 로깅 (DB는 이미 커밋됨)
                        log.error("[Redis 저장 실패] TID: {}, Code: {}, AccountNumber: {}",
                                tid, maskingUtil.maskCode(code), accountNumber, e);
                        // TODO: 보상 트랜잭션 또는 수동 복구 필요
                    }
                }
            });

            // 7. Redirect URL 생성
            String redirectUrl = buildRedirectUrl(request.redirectUrl(), code);
            log.info("[계좌 개설 완료] TID: {}, AccountNumber: {}", request.tid(), account.getAccountNumber());

            return new AccountCreateResDto(redirectUrl);

        } catch (DataIntegrityViolationException e) {
            // 동시성 이슈로 인한 중복 제약 위반 (Race Condition)
            log.error("[계좌 개설 실패 - 중복] TID: {}, Phone: {}, Email: {}, Exception: {}",
                    request.tid(),
                    maskingUtil.maskPhone(session.getPhone()),
                    maskingUtil.maskEmail(session.getEmail()),
                    e.getMessage(),
                    e);

            // 어떤 제약 조건을 위반했는지 다시 확인하여 구체적인 메시지 제공
            try {
                validateUserUniqueness(session);
            } catch (CommonException validationException) {
                // 중복 검증에서 예외가 발생하면 해당 예외를 던짐
                throw validationException;
            }
            // 전화번호, 이메일이 아닌 다른 제약 조건 위반 (예: accountNumber 중복 등)
            throw new CommonException(ErrorCode.CONFLICT,
                    "계좌 개설 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        } catch (Exception e) {
            // 예상치 못한 예외만 INTERNAL_SERVER_ERROR로 변환
            log.error("[계좌 개설 실패] TID: {}, Error: {}", request.tid(), e.getMessage(), e);
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "계좌 개설 중 오류가 발생했습니다");
        }
    }

    /**
     * 사용자 고유성 검증 (전화번호, 이메일 중복 체크)
     */
    private void validateUserUniqueness(AuthSession session) {
        if (bankUserRepository.existsByPhoneNumber(session.getPhone())) {
            throw new CommonException(ErrorCode.CONFLICT, "이미 가입된 전화번호입니다.");
        }
        if (bankUserRepository.existsByEmail(session.getEmail())) {
            throw new CommonException(ErrorCode.CONFLICT, "이미 가입된 이메일입니다.");
        }
    }

    /**
     * 계좌 개설을 위한 세션 검증
     */
    private AuthSession validateSessionForAccountCreation(String tid) {
        AuthSession session = validationUtil.getSessionOrThrow(tid);

        if (!session.isVerified()) {
            throw new CommonException(ErrorCode.FORBIDDEN, "본인인증을 먼저 완료해주세요");
        }

        if (!session.isTermsAgreed()) {
            throw new CommonException(ErrorCode.FORBIDDEN, "약관 동의를 먼저 완료해주세요");
        }

        if (session.getOrgName() == null || session.getEmail() == null) {
            throw new CommonException(ErrorCode.FORBIDDEN, "추가 정보 입력을 먼저 완료해주세요");
        }

        log.debug("[세션 검증 완료] TID: {}", tid);
        return session;
    }

    /**
     * BankUser 생성
     */
    private BankUser createUser(AuthSession session) {
        LocalDate birthDate = LocalDate.parse(session.getBirth(), DateTimeFormatter.ofPattern(BIRTH_DATE_PATTERN));

        BankUser user = BankUser.builder()
                .nameKr(session.getName())
                .email(session.getEmail())
                .phoneNumber(session.getPhone())
                .birth(birthDate)
                .accountCreationTid(session.getTid()) // 계좌 개설 추적용 TID 저장
                .build();

        return bankUserRepository.save(user);
    }

    /**
     * BankAccount 생성
     */
    private BankAccount createBankAccount(BankUser user, String password) {
        String accountNumber = accountNumberGenerator.generate();
        String hashedPassword = passwordEncoder.encode(password); // BCrypt 암호화

        BankAccount account = BankAccount.builder()
                .user(user)
                .accountNumber(accountNumber)
                .password(hashedPassword)
                .balance(0L)
                .build();

        return bankAccountRepository.save(account);
    }

    /**
     * 랜덤 Code 생성 (16자리) with 중복 체크
     * SecureRandom을 사용하여 암호학적으로 안전한 코드 생성
     * Redis에 이미 존재하는 코드는 재생성
     */
    private String generateCode() {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            String code = generateRandomCode();

            // Redis에 이미 존재하는지 확인
            if (redis.getTidByCode(code) == null) {
                return code; // 중복 없음
            }
            log.warn("[Code 중복 감지] 재생성 시도: {}/{}", i + 1, maxRetries);
        }
        throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR,
                "Code 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
    }

    /**
     * 랜덤 문자열 생성 (16자리)
     */
    private String generateRandomCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_CHARS.charAt(SECURE_RANDOM.nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }

    /**
     * Redirect URL 생성
     * UriComponentsBuilder를 사용하여 안전하게 URL 파라미터 추가
     */
    private String buildRedirectUrl(String baseUrl, String code) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("code", code)
                .build()
                .toUriString();
    }
}
