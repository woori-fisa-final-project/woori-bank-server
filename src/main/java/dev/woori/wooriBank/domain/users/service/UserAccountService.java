package dev.woori.wooriBank.domain.users.service;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.config.security.Encoder;
import dev.woori.wooriBank.domain.account.entity.BankAccount;
import dev.woori.wooriBank.domain.account.repository.BankAccountRepository;
import dev.woori.wooriBank.domain.users.dto.CreateUserAccountReqDto;
import dev.woori.wooriBank.domain.users.dto.UserAccountResDto;
import dev.woori.wooriBank.domain.users.entity.BankUser;
import dev.woori.wooriBank.domain.users.repository.BankUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 생성 및 계좌 개설 서비스
 * 메인 서버의 userId를 받아 은행 서버의 BankUser와 BankAccount를 생성
 */
@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final BankUserRepository bankUserRepository;
    private final BankAccountRepository bankAccountRepository;
    private final Encoder encoder;

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

        // 1. auth_token(메인 서버 userId) 중복 체크 - 1인 1계좌 제한
        if (bankUserRepository.existsByAuthToken(externalUserId)) {
            throw new CommonException(ErrorCode.CONFLICT, "이미 은행 계좌가 개설된 사용자입니다.");
        }

        // 2. 이메일 중복 체크
        if (bankUserRepository.existsByEmail(dto.email())) {
            throw new CommonException(ErrorCode.CONFLICT, "이미 등록된 이메일입니다.");
        }

        // 3. 계좌번호 중복 체크
        if (bankAccountRepository.existsByAccountNumber(dto.accountNumber())) {
            throw new CommonException(ErrorCode.CONFLICT, "이미 존재하는 계좌번호입니다.");
        }

        // 4. BankUser 생성 (메인 서버의 userId를 authToken에 저장)
        BankUser bankUser = BankUser.builder()
                .nameKr(dto.nameKr())
                .nameEn(dto.nameEn())
                .email(dto.email())
                .phoneNumber(dto.phoneNumber())
                .birth(dto.birth())
                .authToken(externalUserId)  // 메인 서버의 userId 저장
                .build();

        BankUser savedUser = bankUserRepository.save(bankUser);

        // 5. BankAccount 생성 (계좌 비밀번호는 SHA-256 암호화)
        BankAccount bankAccount = BankAccount.builder()
                .user(savedUser)
                .accountNumber(dto.accountNumber())
                .password(encoder.encode(dto.accountPassword()))  // 비밀번호 암호화
                .balance(dto.initialBalance())
                .build();

        BankAccount savedAccount = bankAccountRepository.save(bankAccount);

        // 6. Response DTO 생성
        return new UserAccountResDto(
                savedUser.getId(),
                savedUser.getNameKr(),
                savedUser.getEmail(),
                savedUser.getPhoneNumber(),
                savedAccount.getId(),
                savedAccount.getAccountNumber(),
                savedAccount.getBalance()
        );
    }
}
