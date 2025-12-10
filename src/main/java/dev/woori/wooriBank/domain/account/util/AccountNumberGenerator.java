package dev.woori.wooriBank.domain.account.util;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.domain.account.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 계좌번호 자동 생성 유틸리티
 * 중복되지 않는 유니크한 계좌번호를 생성합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {

    private final BankAccountRepository bankAccountRepository;

    private static final String ACCOUNT_PREFIX = "1002-999-";
    private static final int MAX_ATTEMPTS = 100;

    /**
     * 계좌번호 자동 생성 (1002-999-XXXXXX)
     * 중복 체크를 통해 유니크한 계좌번호 보장
     * ThreadLocalRandom과 while 루프 사용으로 성능 및 안정성 개선
     *
     * @return 생성된 유니크한 계좌번호
     * @throws CommonException 계좌번호 생성 실패 시
     */
    public String generate() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String random = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
            String accountNumber = ACCOUNT_PREFIX + random;
            if (!bankAccountRepository.existsByAccountNumber(accountNumber)) {
                return accountNumber;
            }
            log.warn("[계좌번호 중복 발생] 재시도 {}/{}", i + 1, MAX_ATTEMPTS);
        }

        log.error("[계좌번호 생성 실패] 최대 시도 횟수 초과");
        throw new CommonException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "계좌번호 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
    }
}
