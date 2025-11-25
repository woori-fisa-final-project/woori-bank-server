package dev.woori.wooriBank.domain.transaction.transfer.service;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
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
 * TransferService — 포인트 현금화를 위한 안정적인 이체 서비스
 * 1) @Transactional → 잔액 수정 + 거래내역 저장 원자성 보장
 * 2) 비관적 락(PESSIMISTIC_WRITE) → 동시성 환경에서도 잔액 깨짐 방지
 * 3) 데드락 방지(계좌번호 정렬 후 락 획득) → A→B, B→A 교착 문제 예방
 * 4) 유효성 검증(동일 계좌 이체 금지) → 잘못된 요청 차단
 * 5) 공통 거래내역 생성 메서드 → 중복 코드 제거 및 유지보수성 향상
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
        // 0. 동일 계좌 이체 금지 (유효성 검증)
        if (request.fromAccount().equals(request.toAccount())) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "보내는 계좌와 받는 계좌가 동일할 수 없습니다.");
        }

        log.info("[입금 요청] from={} to={} amount={}",
                request.fromAccount(), request.toAccount(), request.amount());

        // 1. 데드락 방지: 계좌번호 오름차순 기준으로 락 획득 순서 고정
        String fromAccountNumber = request.fromAccount();
        String toAccountNumber = request.toAccount();

        BankAccount from;
        BankAccount to;

        java.util.function.Supplier<CommonException> fromAccountNotFound =
                () -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "보내는 계좌가 존재하지 않습니다.");
        java.util.function.Supplier<CommonException> toAccountNotFound =
                () -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "받는 계좌가 존재하지 않습니다.");

        if (fromAccountNumber.compareTo(toAccountNumber) < 0) {
            from = bankAccountRepository.findAndLockByAccountNumber(fromAccountNumber).orElseThrow(fromAccountNotFound);
            to = bankAccountRepository.findAndLockByAccountNumber(toAccountNumber).orElseThrow(toAccountNotFound);
        } else {
            to = bankAccountRepository.findAndLockByAccountNumber(toAccountNumber).orElseThrow(toAccountNotFound);
            from = bankAccountRepository.findAndLockByAccountNumber(fromAccountNumber).orElseThrow(fromAccountNotFound);
        }

        long amount = request.amount();

        // 2. 출금/입금 — @Transactional + 비관적 락으로 원자성 보장
        from.withdraw(amount);// 관리자 계좌에서 금액 출금
        to.deposit(amount);// 사용자 계좌에 금액 입금

        // 3. 거래내역 기록 — 공통 메서드로 중복 제거
        createHistory(
                from,
                -amount,
                to.getUser().getNameKr(),
                "포인트 출금",
                "포인트 현금화 출금"
        ); // 관리자 계좌 → 출금 내역
        createHistory(
                to,
                amount,
                from.getUser().getNameKr(),
                "포인트 입금",
                "포인트 현금화 입금"
        );// 사용자 계좌 → 입금 내역

        // 4. 응답 반환
        return TransferResponseDto.builder()
                .fromAccount(from.getAccountNumber())
                .toAccount(to.getAccountNumber())
                .amount(amount)
                .message("이체가 성공적으로 처리되었습니다.")
                .build();
    }

    // 공통 거래내역 생성 메서드
    private void createHistory(
            BankAccount account,
            long amount,
            String counterparty,
            String displayName,
            String description
    ) {
        historyRepository.save(
                BankTransactionHistory.builder()
                        .account(account)
                        .amount(amount)
                        .counterpartyName(counterparty)
                        .displayName(displayName)
                        .description(description)
                        .build()
        );

    }
}
