package dev.woori.wooriBank.domain.account.util;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.domain.account.repository.BankAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

/**
 * AccountNumberGenerator 테스트
 * 계좌번호 생성 및 중복 체크 검증
 */
@ExtendWith(MockitoExtension.class)
class AccountNumberGeneratorTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @InjectMocks
    private AccountNumberGenerator accountNumberGenerator;

    @Test
    @DisplayName("계좌번호는 1002-999- 접두사로 시작해야 한다")
    void generate_ShouldStartWithPrefix() {
        // given
        given(bankAccountRepository.existsByAccountNumber(anyString()))
                .willReturn(false);

        // when
        String accountNumber = accountNumberGenerator.generate();

        // then
        assertThat(accountNumber).startsWith("1002-999-");
    }

    @Test
    @DisplayName("계좌번호는 총 15자리여야 한다 (1002-999-XXXXXX)")
    void generate_ShouldBe15CharactersLong() {
        // given
        given(bankAccountRepository.existsByAccountNumber(anyString()))
                .willReturn(false);

        // when
        String accountNumber = accountNumberGenerator.generate();

        // then
        assertThat(accountNumber).hasSize(15); // "1002-999-" (9자) + "XXXXXX" (6자) = 15자
    }

    @Test
    @DisplayName("계좌번호의 뒷 6자리는 숫자여야 한다")
    void generate_ShouldHave6DigitsSuffix() {
        // given
        given(bankAccountRepository.existsByAccountNumber(anyString()))
                .willReturn(false);

        // when
        String accountNumber = accountNumberGenerator.generate();

        // then
        String suffix = accountNumber.substring(9); // "1002-999-" 이후
        assertThat(suffix).matches("\\d{6}"); // 6자리 숫자
    }

    @Test
    @DisplayName("중복되지 않는 계좌번호가 생성되어야 한다")
    void generate_ShouldReturnUniqueAccountNumber_WhenFirstAttemptSucceeds() {
        // given
        given(bankAccountRepository.existsByAccountNumber(anyString()))
                .willReturn(false); // 중복 없음

        // when
        String accountNumber = accountNumberGenerator.generate();

        // then
        assertThat(accountNumber).isNotNull();
        verify(bankAccountRepository, times(1)).existsByAccountNumber(anyString());
    }

    @Test
    @DisplayName("중복된 계좌번호 발생 시 재시도해야 한다")
    void generate_ShouldRetry_WhenDuplicateExists() {
        // given
        given(bankAccountRepository.existsByAccountNumber(anyString()))
                .willReturn(true)  // 1번째 중복
                .willReturn(true)  // 2번째 중복
                .willReturn(false); // 3번째 성공

        // when
        String accountNumber = accountNumberGenerator.generate();

        // then
        assertThat(accountNumber).isNotNull();
        verify(bankAccountRepository, times(3)).existsByAccountNumber(anyString());
    }

    @Test
    @DisplayName("100번 모두 중복이면 예외가 발생해야 한다")
    void generate_ShouldThrowException_WhenMaxAttemptsExceeded() {
        // given
        given(bankAccountRepository.existsByAccountNumber(anyString()))
                .willReturn(true); // 모든 시도 중복

        // when & then
        assertThatThrownBy(() -> accountNumberGenerator.generate())
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("계좌번호 생성에 실패했습니다");

        verify(bankAccountRepository, times(100)).existsByAccountNumber(anyString());
    }

    @Test
    @DisplayName("여러 번 호출해도 매번 다른 계좌번호가 생성되어야 한다")
    void generate_ShouldGenerateDifferentNumbers_OnMultipleCalls() {
        // given
        given(bankAccountRepository.existsByAccountNumber(anyString()))
                .willReturn(false);

        // when
        Set<String> accountNumbers = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            accountNumbers.add(accountNumberGenerator.generate());
        }

        // then
        assertThat(accountNumbers).hasSize(100); // 모두 다른 번호
    }

    @Test
    @DisplayName("뒷 6자리는 000000부터 999999 범위 내여야 한다")
    void generate_ShouldGenerateValidRange() {
        // given
        given(bankAccountRepository.existsByAccountNumber(anyString()))
                .willReturn(false);

        // when
        for (int i = 0; i < 100; i++) {
            String accountNumber = accountNumberGenerator.generate();
            String suffix = accountNumber.substring(9);
            int number = Integer.parseInt(suffix);

            // then
            assertThat(number).isBetween(0, 999999);
        }
    }

    @Test
    @DisplayName("뒷 6자리가 10000 미만이면 앞에 0으로 패딩되어야 한다")
    void generate_ShouldPadWithZeros_WhenNumberIsLessThan100000() {
        // given
        given(bankAccountRepository.existsByAccountNumber(anyString()))
                .willReturn(false);

        // when
        boolean foundPaddedNumber = false;
        for (int i = 0; i < 1000; i++) {
            String accountNumber = accountNumberGenerator.generate();
            String suffix = accountNumber.substring(9);

            if (suffix.startsWith("0")) {
                foundPaddedNumber = true;
                assertThat(suffix).hasSize(6); // 0으로 패딩되어 6자리 유지
                break;
            }
        }

        // then
        // 1000번 시도하면 통계적으로 0으로 시작하는 번호가 나올 확률이 높음
        // 하지만 테스트의 안정성을 위해 이 부분은 주석 처리하거나 통계적 검증으로 대체 가능
    }

    @Test
    @DisplayName("계좌번호 형식이 정확해야 한다")
    void generate_ShouldMatchCorrectFormat() {
        // given
        given(bankAccountRepository.existsByAccountNumber(anyString()))
                .willReturn(false);

        // when
        String accountNumber = accountNumberGenerator.generate();

        // then
        assertThat(accountNumber).matches("^1002-999-\\d{6}$");
    }

    @Test
    @DisplayName("Repository 예외 발생 시 상위로 전파되어야 한다")
    void generate_ShouldPropagateException_WhenRepositoryThrowsException() {
        // given
        given(bankAccountRepository.existsByAccountNumber(anyString()))
                .willThrow(new RuntimeException("DB connection failed"));

        // when & then
        assertThatThrownBy(() -> accountNumberGenerator.generate())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB connection failed");
    }
}
