package dev.woori.wooriBank.domain.transaction.transfer.service;

import dev.woori.wooriBank.domain.account.entity.BankAccount;
import dev.woori.wooriBank.domain.account.repository.BankAccountRepository;
import dev.woori.wooriBank.domain.transaction.transfer.dto.TransferRequestDto;
import dev.woori.wooriBank.domain.transaction.transfer.dto.TransferResponseDto;
import dev.woori.wooriBank.domain.transaction.transfer.repository.BankTransactionHistoryRepository;
import dev.woori.wooriBank.domain.transaction.entity.BankTransactionHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 입금 기능의 핵심 비즈니스 로직을 처리하는 Service 클래스.
 *
 * @Transactional: 입금 과정(계좌조회 → 잔액변경 → 거래내역 저장)이 모두 하나의 트랜잭션으로 처리됨.
 *                 중간에 오류가 나면 전체 과정을 롤백하여 데이터 일관성을 유지한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final BankAccountRepository bankAccountRepository;
    private final BankTransactionHistoryRepository historyRepository;

    /**
     * 포인트 현금화 이체 기능
     * fromAccount → toAccount 로 금액 이동
     */
    @Transactional
    public TransferResponseDto transfer(TransferRequestDto request) {

        log.info("[계좌이체 요청] from={} to={} amount={}",
                request.fromAccount(), request.toAccount(), request.amount());

        // 1. 보내는 계좌(관리자 계좌) + 락
        BankAccount from = bankAccountRepository
                .findAndLockByAccountNumber(request.fromAccount())
                .orElseThrow(() -> new IllegalArgumentException("보내는 계좌가 존재하지 않습니다."));

        // 2. 받는 계좌(사용자 계좌) 조회 + 락
        BankAccount to = bankAccountRepository
                .findAndLockByAccountNumber(request.toAccount())
                .orElseThrow(() -> new IllegalArgumentException("받는 계좌가 존재하지 않습니다."));

        int amount = request.amount();

        // 3. 관리자 계좌에서 금액 출금
        from.withdraw(amount);

        // 4. 사용자 계좌에 금액 입금
        to.deposit(amount);

        // 5. 거래내역 생성: 관리자 계좌 → 출금 내역
        historyRepository.save(
                BankTransactionHistory.builder()
                        .account(from)
                        .amount(-amount)
                        .counterpartyName("User:" + to.getAccountNumber())
                        .displayName("포인트 출금")
                        .description("포인트 현금화 출금")
                        .build()
        );

        // 6. 거래내역 생성: 사용자 계좌 → 입금 내역
        historyRepository.save(
                BankTransactionHistory.builder()
                        .account(to)
                        .amount(amount)
                        .counterpartyName("Admin:" + from.getAccountNumber())
                        .displayName("포인트 입금")
                        .description("포인트 현금화 입금")
                        .build()
        );

        return TransferResponseDto.builder()
                .fromAccount(from.getAccountNumber())
                .toAccount(to.getAccountNumber())
                .amount(amount)
                .message("이체가 성공적으로 처리되었습니다.")
                .build();
    }
}
