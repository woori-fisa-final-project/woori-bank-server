package dev.woori.wooriBank.domain.account.service;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.domain.account.dto.request.AccountCreateReqDto;
import dev.woori.wooriBank.domain.account.dto.request.AccountFormReqDto;
import dev.woori.wooriBank.domain.account.dto.request.AccountLookupReqDto;
import dev.woori.wooriBank.domain.account.dto.response.AccountCreateResDto;
import dev.woori.wooriBank.domain.account.dto.response.AccountFormResDto;
import dev.woori.wooriBank.domain.account.dto.response.AccountLookupResDto;
import dev.woori.wooriBank.domain.account.dto.response.TidResDto;
import dev.woori.wooriBank.domain.account.entity.BankAccount;
import dev.woori.wooriBank.domain.account.repository.BankAccountRepository;
import dev.woori.wooriBank.domain.account.util.AccountNumberGenerator;
import dev.woori.wooriBank.domain.auth.entity.AuthSession;
import dev.woori.wooriBank.domain.auth.entity.AuthStoreRedis;
import dev.woori.wooriBank.domain.auth.repository.BankClientAppRepository;
import dev.woori.wooriBank.domain.users.entity.BankUser;
import dev.woori.wooriBank.domain.users.repository.BankUserRepository;
import dev.woori.wooriBank.domain.util.EncryptionUtil;
import dev.woori.wooriBank.domain.util.HashUtil;
import dev.woori.wooriBank.domain.util.MaskingUtil;
import dev.woori.wooriBank.domain.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
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
    private final EncryptionUtil encryptionUtil;
    private final HashUtil hashUtil;
    private final BankUserRepository bankUserRepository;
    private final BankClientAppRepository bankClientAppRepository;
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
        session.setEngName(request.engName());
        session.setEmail(request.email());

        // 4. Redis 저장
        redis.save(request.tid(), session);

        log.info("[추가 정보 저장 완료] TID: {}, 영어 이름: {}, 이메일: {}",
                request.tid(), request.engName(), maskingUtil.maskEmail(request.email()));

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

        // 6. 계좌 정보 반환 (암호화된 이름 복호화)
        String decryptedName = encryptionUtil.decrypt(session.getName());
        return new AccountLookupResDto(
                decryptedName,
                account.getAccountNumber()
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

        // 2. Code 생성 및 Redis에 원자적으로 선점 (SETNX)
        // 동시성 문제 해결: 코드 생성 시점에 즉시 Redis에 저장
        String code = generateCode(request.tid());
        log.info("[Code 생성 및 선점 완료] Code: {}, TID: {}", maskingUtil.maskCode(code), request.tid());

        try {
            // 3. BankUser 생성
            BankUser user = createUser(session);
            log.info("[사용자 생성 완료] UserId: {}, Phone: {}", user.getId(), maskingUtil.maskPhone(user.getPhoneNumber()));

            // 4. BankAccount 생성
            BankAccount account = createBankAccount(user, request.password());
            log.info("[계좌 생성 완료] AccountNumber: {}, UserId: {}", account.getAccountNumber(), user.getId());

            // 5. DB 트랜잭션 커밋 후 Redis 작업 수행
            String tid = request.tid();
            String accountNumber = account.getAccountNumber();

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 세션에 Code 및 계좌번호 저장 (Code는 이미 Redis에 저장됨)
                    session.setCode(code);
                    session.setAccountNumber(accountNumber);

                    // 비동기로 재시도 로직 실행 (메인 스레드 차단 방지)
                    saveSessionWithRetryAsync(tid, session, code, accountNumber);
                }

                @Override
                public void afterCompletion(int status) {
                    // 트랜잭션이 롤백된 경우 (STATUS_ROLLED_BACK)
                    if (status == STATUS_ROLLED_BACK) {
                        try {
                            // Redis에 저장된 Code 삭제
                            redis.deleteCode(code);
                            log.info("[트랜잭션 롤백 - Code 삭제] TID: {}, Code: {}", tid, maskingUtil.maskCode(code));
                        } catch (Exception e) {
                            log.error("[Code 삭제 실패] TID: {}, Code: {}", tid, maskingUtil.maskCode(code), e);
                        }
                    }
                }
            });

            // 6. Redirect URL 생성
            String redirectUrl = bankClientAppRepository.findByClientId(session.getClientId())
                    .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND)).getRedirectUrl();
            log.info("[계좌 개설 완료] TID: {}, AccountNumber: {}", request.tid(), account.getAccountNumber());

            return new AccountCreateResDto(buildRedirectUrl(redirectUrl, code));

        } catch (DataIntegrityViolationException e) {
            // 동시성 이슈로 인한 중복 제약 위반 (Race Condition)
            log.error("[계좌 개설 실패 - 중복] TID: {}, Phone: {}, Email: {}, Exception: {}",
                    request.tid(),
                    maskingUtil.maskPhone(session.getPhone()),
                    maskingUtil.maskEmail(session.getEmail()),
                    e.getMessage(),
                    e);
            throw new CommonException(ErrorCode.CONFLICT,
                    "계좌 개설 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        } catch (Exception e) {
            // 예상치 못한 예외만 INTERNAL_SERVER_ERROR로 변환
            log.error("[계좌 개설 실패] TID: {}, Error: {}", request.tid(), e.getMessage(), e);
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "계좌 개설 중 오류가 발생했습니다");
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

        if (session.getEngName() == null || session.getEmail() == null) {
            throw new CommonException(ErrorCode.FORBIDDEN, "추가 정보 입력을 먼저 완료해주세요");
        }

        log.debug("[세션 검증 완료] TID: {}", tid);
        return session;
    }

    /**
     * BankUser 생성
     *
     * 보안 구조:
     * 1. Redis에서 암호화된 데이터 가져오기
     * 2. 복호화하여 평문으로 변환
     * 3. RRN 해시값으로 기존 사용자 검색
     * 4. 신규 사용자 생성 시:
     *    - 평문 데이터를 엔티티에 저장
     *    - JPA Converter가 자동으로 DB 암호화
     *    - RRN 해시값 함께 저장 (검색용)
     */
    private BankUser createUser(AuthSession session) {
        // Redis에서 가져온 암호화된 데이터를 복호화
        String decryptedRrn = encryptionUtil.decrypt(session.getRrn());
        String decryptedName = encryptionUtil.decrypt(session.getName());
        String decryptedPhone = encryptionUtil.decrypt(session.getPhone());
        String decryptedBirth = encryptionUtil.decrypt(session.getBirth());

        // RRN 해시값으로 기존 사용자 검색 (암호화된 DB 조회)
        String rrnHash = hashUtil.sha256(decryptedRrn);
        BankUser registeredUser = bankUserRepository.findByRrnHash(rrnHash).orElse(null);

        // 회원으로 등록되어 있으면 해당 정보 return
        if (registeredUser != null) {
            return registeredUser;
        }

        // 회원으로 등록되어 있지 않으면 회원가입 진행
        LocalDate birthDate = LocalDate.parse(decryptedBirth, DateTimeFormatter.ofPattern(BIRTH_DATE_PATTERN));
        BankUser user = BankUser.builder()
                .rrn(decryptedRrn)          // JPA Converter가 암호화하여 DB 저장
                .rrnHash(rrnHash)           // 검색용 해시값 (평문 저장)
                .nameKr(decryptedName)      // JPA Converter가 암호화하여 DB 저장
                .nameEn(session.getEngName())
                .email(session.getEmail())
                .phoneNumber(decryptedPhone) // JPA Converter가 암호화하여 DB 저장
                .birth(birthDate)
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
     * 랜덤 Code 생성 및 원자적 선점 (16자리)
     * SecureRandom을 사용하여 암호학적으로 안전한 코드 생성
     * Redis SETNX를 사용하여 동시성 문제 해결
     *
     * @param tid Code와 매핑할 TID
     * @return 생성되고 Redis에 선점된 Code
     */
    private String generateCode(String tid) {
        int maxRetries = 10; // SETNX 실패 시 재시도 횟수 증가
        for (int i = 0; i < maxRetries; i++) {
            String code = generateRandomCode();

            // Redis에 원자적으로 저장 (SETNX)
            // 존재하지 않는 경우에만 저장되므로 동시성 문제 해결
            if (redis.setCodeIfAbsent(code, tid, 600L)) { // 10분 TTL
                log.debug("[Code 생성 성공] Code: {}, TID: {}", maskingUtil.maskCode(code), tid);
                return code; // 선점 성공
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

    /**
     * Redis 세션 저장 재시도 로직 (비동기 보상 트랜잭션)
     *
     * 재시도 전략: Exponential Backoff
     * - 1차 실패 후: 100ms 대기
     * - 2차 실패 후: 200ms 대기
     * - 3차 실패 시: 최종 실패 (알림 필요)
     *
     * 비동기 처리:
     * - @Async로 별도 스레드에서 실행
     * - 메인 요청 스레드를 차단하지 않음
     * - 실패 시 로깅 및 알림만 수행 (메인 응답은 정상)
     *
     * @param tid Transaction ID
     * @param session 저장할 세션 객체
     * @param code 마스킹용 코드 (평문)
     * @param accountNumber 마스킹용 계좌번호 (평문)
     */
    @Async
    public void saveSessionWithRetryAsync(String tid, AuthSession session, String code, String accountNumber) {
        int maxRetries = 3;
        int baseDelayMs = 100;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                redis.save(tid, session);
                log.info("[비동기 세션 업데이트 완료] TID: {}, Code: {}, 시도: {}/{}",
                        tid, maskingUtil.maskCode(code), attempt, maxRetries);
                return; // 성공 시 종료
            } catch (Exception e) {
                log.warn("[비동기 세션 저장 실패 - 재시도 {}/{}] TID: {}, Code: {}, AccountNumber: {}, Error: {}",
                        attempt, maxRetries, tid, maskingUtil.maskCode(code), accountNumber, e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        // Exponential backoff: 100ms -> 200ms
                        long delay = (long) (Math.pow(2, attempt - 1) * baseDelayMs);
                        Thread.sleep(delay); // 비동기 스레드이므로 메인 요청에 영향 없음
                        log.debug("[비동기 재시도 대기] {}ms 후 {}번째 시도", delay, attempt + 1);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("[비동기 재시도 인터럽트] TID: {}", tid);
                        sendFailureAlert(tid, code, accountNumber, e);
                        return;
                    }
                } else {
                    // 최종 실패 시 알림
                    log.error("[비동기 세션 저장 최종 실패] TID: {}, Code: {}, AccountNumber: {}",
                            tid, maskingUtil.maskCode(code), accountNumber, e);
                    sendFailureAlert(tid, code, accountNumber, e);
                }
            }
        }
    }

    /**
     * 세션 저장 최종 실패 시 알림 전송
     * TODO: 실제 알림 시스템 연동 (이메일, Slack, SMS 등)
     */
    private void sendFailureAlert(String tid, String code, String accountNumber, Exception e) {
        // TODO: 알림 시스템 연동
        log.error("===== 세션 저장 최종 실패 알림 =====");
        log.error("TID: {}", tid);
        log.error("Code: {}", maskingUtil.maskCode(code));
        log.error("AccountNumber: {}", accountNumber);
        log.error("Error: ", e);
        log.error("==================================");
    }
}
