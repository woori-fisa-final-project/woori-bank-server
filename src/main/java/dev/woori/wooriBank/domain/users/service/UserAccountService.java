package dev.woori.wooriBank.domain.users.service;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.domain.account.dto.response.UserAccountResDto;
import dev.woori.wooriBank.domain.account.entity.BankAccount;
import dev.woori.wooriBank.domain.account.repository.BankAccountRepository;
import dev.woori.wooriBank.domain.account.util.AccountNumberGenerator;
import dev.woori.wooriBank.domain.users.dto.CreateUserAccountReqDto;
import dev.woori.wooriBank.domain.users.entity.BankUser;
import dev.woori.wooriBank.domain.users.repository.BankUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 생성 및 계좌 개설 서비스
 * 메인 서버의 userId를 받아 은행 서버의 BankUser와 BankAccount를 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final BankUserRepository bankUserRepository;
    private final BankAccountRepository bankAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountNumberGenerator accountNumberGenerator;

    /**
     * 회원 생성 + 계좌 개설 (1인 1계좌)
     *
     * @param tid 거래 추적 ID (TID)
     * @param dto 회원 및 계좌 정보
     * @return 생성된 회원 및 계좌 정보
     * @throws CommonException CONFLICT - 이미 계좌가 있는 사용자, 이메일 중복, 계좌번호 중복
     */
    @Transactional
    public UserAccountResDto createUserWithAccount(String tid, CreateUserAccountReqDto dto) {

        // 1. 사전 검증 (명확성)
        validateUserUniqueness(tid, dto.email());

        try {
            // 2. BankUser 생성 (TID 저장)
            BankUser bankUser = BankUser.builder()
                    .nameKr(dto.nameKr())
                    .nameEn(dto.nameEn())
                    .email(dto.email())
                    .phoneNumber(dto.phoneNumber())
                    .birth(dto.birth())
                    .accountCreationTid(tid)  // TID 저장
                    .build();

            BankUser savedUser = bankUserRepository.save(bankUser);

            // 3. 계좌번호 자동 생성
            String accountNumber = accountNumberGenerator.generate();

            // 4. BankAccount 생성 (계좌 PIN은 BCrypt 암호화)
            BankAccount bankAccount = BankAccount.builder()
                    .user(savedUser)
                    .accountNumber(accountNumber)
                    .password(passwordEncoder.encode(dto.accountPin()))  // BCrypt 암호화
                    .balance(dto.initialBalance())  // Long 타입 그대로 사용
                    .build();

            BankAccount savedAccount = bankAccountRepository.save(bankAccount);

            // 5. Response DTO 생성
            return new UserAccountResDto(
                    savedUser.getId(),
                    savedUser.getNameKr(),
                    savedUser.getEmail(),
                    savedUser.getPhoneNumber(),
                    savedAccount.getId(),
                    savedAccount.getAccountNumber(),
                    savedAccount.getBalance()
            );
        } catch (DataIntegrityViolationException e) {
            // 동시성 문제로 인한 중복 발생 시 (최종 안전장치)
            log.error("DataIntegrityViolationException 발생 - tid: {}, email: {}",
                    tid, dto.email(), e);

            // 어떤 제약 조건을 위반했는지 다시 확인하여 구체적인 메시지 제공
            try {
                validateUserUniqueness(tid, dto.email());
            } catch (CommonException validationException) {
                // 중복 검증에서 예외가 발생하면 해당 예외를 던짐
                throw validationException;
            }
            // TID, email이 아닌 다른 제약 조건 위반
            throw new CommonException(ErrorCode.CONFLICT,
                    "계좌 개설 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    /**
     * 사용자 고유성 검증 (TID, 이메일 중복 체크)
     */
    private void validateUserUniqueness(String tid, String email) {
        if (bankUserRepository.existsByAccountCreationTid(tid)) {
            throw new CommonException(ErrorCode.CONFLICT, "이미 은행 계좌가 개설된 사용자입니다.");
        }
        if (bankUserRepository.existsByEmail(email)) {
            throw new CommonException(ErrorCode.CONFLICT, "이미 등록된 이메일입니다.");
        }
    }
}
