package dev.woori.wooriBank.domain.account.service;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.domain.account.dto.request.AccountCreateDto;
import dev.woori.wooriBank.domain.account.dto.response.AccountResponse;
import dev.woori.wooriBank.domain.account.entity.BankAccount;
import dev.woori.wooriBank.domain.account.repository.BankAccountRepository;
import dev.woori.wooriBank.domain.users.entity.BankUser;
import dev.woori.wooriBank.domain.users.repository.BankUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 계좌 개설 서비스
 * 명세서: POST /api/account/create
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final BankUserRepository bankUserRepository;
    private final BankAccountRepository bankAccountRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 계좌 개설 (명세서 버전)
     *
     * @param externalUserId 메인 서버의 사용자 ID (Header에서 전달)
     * @param dto 계좌 개설 요청 정보
     * @return 생성된 계좌 정보
     */
    @Transactional
    public AccountResponse createAccount(String externalUserId, AccountCreateDto dto) {

        // 1. 사전 검증
        if (bankUserRepository.existsByAuthToken(externalUserId)) {
            throw new CommonException(ErrorCode.CONFLICT, "이미 은행 계좌가 개설된 사용자입니다.");
        }

        if (bankUserRepository.existsByEmail(dto.email())) {
            throw new CommonException(ErrorCode.CONFLICT, "이미 등록된 이메일입니다.");
        }

        try {
            // 2. BankUser 생성
            LocalDate birthDate = LocalDate.parse(dto.birth(), DateTimeFormatter.ofPattern("yyyyMMdd"));

            BankUser bankUser = BankUser.builder()
                    .nameKr(dto.name())
                    .email(dto.email())
                    .phoneNumber(dto.phone())
                    .birth(birthDate)
                    .authToken(externalUserId)
                    .build();

            BankUser savedUser = bankUserRepository.save(bankUser);

            // 3. 계좌번호 자동 생성
            String accountNumber = generateAccountNumber();

            // 4. BankAccount 생성
            BankAccount bankAccount = BankAccount.builder()
                    .user(savedUser)
                    .accountNumber(accountNumber)
                    .password(passwordEncoder.encode(dto.password()))
                    .balance(BigDecimal.ZERO)  // 초기 잔액 0원
                    .build();

            BankAccount savedAccount = bankAccountRepository.save(bankAccount);

            // 5. Response 생성
            return new AccountResponse(
                    savedAccount.getAccountNumber(),
                    "교육용 계좌",
                    savedAccount.getBalance(),
                    savedAccount.getCreatedAt()
            );

        } catch (DataIntegrityViolationException e) {
            // 동시성 문제로 인한 중복 발생 시 (최종 안전장치)
            throw new CommonException(ErrorCode.CONFLICT, "중복된 데이터가 존재합니다.");
        }
    }

    /**
     * 계좌번호 자동 생성 (1002-999-XXXXXX)
     * 중복 체크를 통해 유니크한 계좌번호 보장
     * ThreadLocalRandom과 while 루프 사용으로 성능 및 안정성 개선
     */
    private String generateAccountNumber() {
        String prefix = "1002-999-";
        String accountNumber;
        int attempts = 0;
        final int MAX_ATTEMPTS = 100; // 무한 루프 방지

        do {
            if (attempts++ > MAX_ATTEMPTS) {
                throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "계좌번호 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
            }
            String random = String.format("%06d",
                ThreadLocalRandom.current().nextInt(1000000));
            accountNumber = prefix + random;
        } while (bankAccountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }
}
