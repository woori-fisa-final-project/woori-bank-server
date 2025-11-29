package dev.woori.wooriBank.domain.transfer.service;
import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.domain.account.entity.BankAccount;
import dev.woori.wooriBank.domain.account.repository.BankAccountRepository;
import dev.woori.wooriBank.domain.transaction.transfer.dto.TransferRequestDto;
import dev.woori.wooriBank.domain.transaction.transfer.dto.TransferResponseDto;
import dev.woori.wooriBank.domain.transaction.transfer.repository.BankTransactionHistoryRepository;
import dev.woori.wooriBank.domain.transaction.transfer.service.TransferService;
import dev.woori.wooriBank.domain.users.entity.BankUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @InjectMocks
    private TransferService transferService;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private BankTransactionHistoryRepository historyRepository;

    // 테스트를 위한 더미 데이터 생성 헬퍼
    private BankAccount createAccount(String accountNumber, long balance, String userName) {
        BankUser user = BankUser.builder().nameKr(userName).build(); // BankUser Mocking 필요 시 수정
        return BankAccount.builder()
                .accountNumber(accountNumber)
                .balance(balance)
                .user(user)
                .password("1234")
                .build();
    }

    @Test
    @DisplayName("이체 성공: 정상적으로 잔액이 이동하고 내역이 저장된다.")
    void transfer_success() {
        // given
        String fromAccNum = "1000-1111"; // 관리자
        String toAccNum = "2000-2222";   // 사용자
        long amount = 5000L;

        BankAccount fromAccount = createAccount(fromAccNum, 10000L, "관리자");
        BankAccount toAccount = createAccount(toAccNum, 0L, "사용자");

        TransferRequestDto request = TransferRequestDto.builder()
                .fromAccount(fromAccNum)
                .toAccount(toAccNum)
                .amount(amount)
                .build();

        // Mocking: 계좌 조회 시 Lock을 걸고 가져오는 동작 시뮬레이션
        given(bankAccountRepository.findAndLockByAccountNumber(fromAccNum)).willReturn(Optional.of(fromAccount));
        given(bankAccountRepository.findAndLockByAccountNumber(toAccNum)).willReturn(Optional.of(toAccount));

        // when
        TransferResponseDto response = transferService.transfer(request);

        // then
        // 1. 잔액 검증
        assertThat(fromAccount.getBalance()).isEqualTo(5000L); // 10000 - 5000
        assertThat(toAccount.getBalance()).isEqualTo(5000L);   // 0 + 5000

        // 2. 응답값 검증
        assertThat(response.amount()).isEqualTo(amount);
        assertThat(response.fromAccount()).isEqualTo(fromAccNum);
        assertThat(response.toAccount()).isEqualTo(toAccNum);

        // 3. 거래내역 저장 호출 횟수 검증 (보내는 사람, 받는 사람 총 2번 저장되어야 함)
        verify(historyRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("이체 실패: 잔액이 부족하면 예외가 발생한다.")
    void transfer_fail_insufficient_balance() {
        // given
        String fromAccNum = "1000-1111";
        String toAccNum = "2000-2222";
        long amount = 20000L; // 잔액보다 큰 금액

        BankAccount fromAccount = createAccount(fromAccNum, 10000L, "관리자");
        BankAccount toAccount = createAccount(toAccNum, 0L, "사용자");

        TransferRequestDto request = TransferRequestDto.builder()
                .fromAccount(fromAccNum)
                .toAccount(toAccNum)
                .amount(amount)
                .build();

        given(bankAccountRepository.findAndLockByAccountNumber(fromAccNum)).willReturn(Optional.of(fromAccount));
        given(bankAccountRepository.findAndLockByAccountNumber(toAccNum)).willReturn(Optional.of(toAccount));

        // when & then
        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(CommonException.class)
                .hasMessage("잔액이 부족합니다.");
    }

    @Test
    @DisplayName("이체 실패: 보내는 계좌와 받는 계좌가 동일하면 예외가 발생한다.")
    void transfer_fail_same_account() {
        // given
        String accNum = "1000-1111";
        TransferRequestDto request = TransferRequestDto.builder()
                .fromAccount(accNum)
                .toAccount(accNum)
                .amount(1000L)
                .build();

        // when & then
        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("동일할 수 없습니다");
    }

    @Test
    @DisplayName("이체 실패: 계좌가 존재하지 않으면 예외가 발생한다.")
    void transfer_fail_account_not_found() {
        // given
        String fromAccNum = "1000-1111";
        String toAccNum = "9999-9999"; // 존재하지 않는 계좌

        TransferRequestDto request = TransferRequestDto.builder()
                .fromAccount(fromAccNum)
                .toAccount(toAccNum)
                .amount(1000L)
                .build();

        // 데드락 방지 로직상 "1000..." < "9999..." 이므로 from 먼저 조회
        given(bankAccountRepository.findAndLockByAccountNumber(fromAccNum))
                .willReturn(Optional.of(createAccount(fromAccNum, 10000L, "관리자")));
        given(bankAccountRepository.findAndLockByAccountNumber(toAccNum))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("받는 계좌가 존재하지 않습니다");
    }

    @Test
    @DisplayName("데드락 방지: 계좌번호 순서대로 Lock을 획득한다 (From < To)")
    void deadlock_prevention_order_normal() {
        // given
        String smallNum = "1000";
        String bigNum = "2000";

        BankAccount smallAcc = createAccount(smallNum, 10000L, "A");
        BankAccount bigAcc = createAccount(bigNum, 10000L, "B");

        TransferRequestDto request = TransferRequestDto.builder()
                .fromAccount(smallNum)
                .toAccount(bigNum)
                .amount(1000L)
                .build();

        given(bankAccountRepository.findAndLockByAccountNumber(smallNum)).willReturn(Optional.of(smallAcc));
        given(bankAccountRepository.findAndLockByAccountNumber(bigNum)).willReturn(Optional.of(bigAcc));

        // when
        transferService.transfer(request);

        // then
        // 순서 검증: 작은 번호(smallNum) 먼저 조회 후 큰 번호(bigNum) 조회
        InOrder inOrder = inOrder(bankAccountRepository);
        inOrder.verify(bankAccountRepository).findAndLockByAccountNumber(smallNum);
        inOrder.verify(bankAccountRepository).findAndLockByAccountNumber(bigNum);
    }

    @Test
    @DisplayName("데드락 방지: 계좌번호 순서대로 Lock을 획득한다 (From > To)")
    void deadlock_prevention_order_reverse() {
        // given
        String bigNum = "2000"; // From
        String smallNum = "1000"; // To

        BankAccount bigAcc = createAccount(bigNum, 10000L, "B");
        BankAccount smallAcc = createAccount(smallNum, 10000L, "A");

        TransferRequestDto request = TransferRequestDto.builder()
                .fromAccount(bigNum)
                .toAccount(smallNum)
                .amount(1000L)
                .build();

        given(bankAccountRepository.findAndLockByAccountNumber(bigNum)).willReturn(Optional.of(bigAcc));
        given(bankAccountRepository.findAndLockByAccountNumber(smallNum)).willReturn(Optional.of(smallAcc));

        // when
        transferService.transfer(request);

        // then
        // 순서 검증: 작은 번호(smallNum, 즉 To) 먼저 조회 후 큰 번호(bigNum, 즉 From) 조회
        InOrder inOrder = inOrder(bankAccountRepository);
        inOrder.verify(bankAccountRepository).findAndLockByAccountNumber(smallNum);
        inOrder.verify(bankAccountRepository).findAndLockByAccountNumber(bigNum);
    }
}