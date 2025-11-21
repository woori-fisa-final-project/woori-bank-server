package dev.woori.wooriBank.domain.users.service;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.domain.account.entity.BankAccount;
import dev.woori.wooriBank.domain.account.repository.BankAccountRepository;
import dev.woori.wooriBank.domain.users.dto.CreateUserAccountReqDto;
import dev.woori.wooriBank.domain.users.dto.UserAccountResDto;
import dev.woori.wooriBank.domain.users.entity.BankUser;
import dev.woori.wooriBank.domain.users.repository.BankUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

/**
 * 회원 생성 및 계좌 개설 서비스
 * 메인 서버의 userId를 받아 은행 서버의 BankUser와 BankAccount를 생성
 */
@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final BankUserRepository bankUserRepository;
    private final BankAccountRepository bankAccountRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원 생성 + 계좌 개설 (1인 1계좌)
     *
     * @param externalUserId 메인 서버의 userId (bank_users.auth_token에 저장)
     * @param dto 회원 및 계좌 정보
     * @return 생성된 회원 및 계좌 정보
     * @throws CommonException CONFLICT - 이미 계좌가 있는 사용자, 이메일 중복, 계좌번호 중복
     */
    @Transactional
    public UserAccountResDto createUserWithAccount(String externalUserId, CreateUserAccountReqDto dto) {

        // 1. 사전 검증 (명확성)
        if (bankUserRepository.existsByAuthToken(externalUserId)) {
            throw new CommonException(ErrorCode.CONFLICT, "이미 은행 계좌가 개설된 사용자입니다.");
        }

        if (bankUserRepository.existsByEmail(dto.email())) {
            throw new CommonException(ErrorCode.CONFLICT, "이미 등록된 이메일입니다.");
        }

        try {
            // 2. BankUser 생성 (메인 서버의 userId를 authToken에 저장)
            BankUser bankUser = BankUser.builder()
                    .nameKr(dto.nameKr())
                    .nameEn(dto.nameEn())
                    .email(dto.email())
                    .phoneNumber(dto.phoneNumber())
                    .birth(dto.birth())
                    .authToken(externalUserId)  // 메인 서버의 userId 저장
                    .build();

            BankUser savedUser = bankUserRepository.save(bankUser);

            // 3. 계좌번호 자동 생성
            String accountNumber = generateAccountNumber();

            // 4. BankAccount 생성 (계좌 PIN은 BCrypt 암호화)
            BankAccount bankAccount = BankAccount.builder()
                    .user(savedUser)
                    .accountNumber(accountNumber)
                    .password(passwordEncoder.encode(dto.accountPin()))  // BCrypt 암호화
                    .balance(dto.initialBalance())
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
            throw new CommonException(ErrorCode.CONFLICT, "중복된 데이터가 존재합니다.");
        }
    }

    /**
     * 계좌번호 자동 생성 (1002-999-XXXXXX)
     * 중복 체크를 통해 유니크한 계좌번호 보장
     */
    private String generateAccountNumber() {
        String prefix = "1002-999-";
        String random = String.format("%06d", new Random().nextInt(1000000));
        String accountNumber = prefix + random;

        // 중복 체크 (재귀 호출)
        if (bankAccountRepository.existsByAccountNumber(accountNumber)) {
            return generateAccountNumber();
        }

        return accountNumber;
    }
}
