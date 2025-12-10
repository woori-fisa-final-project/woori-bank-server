package dev.woori.wooriBank.domain.account.service;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.domain.auth.entity.AuthStoreRedis;
import dev.woori.wooriBank.domain.util.MaskingUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * AccountService의 Code 생성 동시성 처리 테스트
 * Redis SETNX를 사용한 원자적 Code 선점 검증
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceCodeGenerationTest {

    @Mock
    private AuthStoreRedis redis;

    @Mock
    private MaskingUtil maskingUtil;

    @InjectMocks
    private AccountService accountService;

    /**
     * Reflection을 사용하여 private 메서드 테스트
     * InvocationTargetException을 언래핑하여 실제 예외를 전파
     */
    private String invokeGenerateCode(String tid) throws Exception {
        try {
            Method method = AccountService.class.getDeclaredMethod("generateCode", String.class);
            method.setAccessible(true);
            return (String) method.invoke(accountService, tid);
        } catch (InvocationTargetException e) {
            // 원본 예외를 언래핑하여 다시 던짐
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    @Test
    @DisplayName("Code 생성 시 첫 번째 시도에서 성공하면 해당 Code를 반환해야 한다")
    void generateCode_ShouldReturnCode_WhenFirstAttemptSucceeds() throws Exception {
        // given
        String tid = "test-tid-123";
        given(redis.setCodeIfAbsent(anyString(), eq(tid), eq(600L)))
                .willReturn(true); // 첫 번째 시도에서 성공

        // when
        String code = invokeGenerateCode(tid);

        // then
        assertThat(code).isNotNull();
        assertThat(code).hasSize(16); // 16자리 코드
        verify(redis, times(1)).setCodeIfAbsent(anyString(), eq(tid), eq(600L));
    }

    @Test
    @DisplayName("Code 중복 시 재생성하여 최대 10번 시도해야 한다")
    void generateCode_ShouldRetry_WhenCodeAlreadyExists() throws Exception {
        // given
        String tid = "test-tid-123";
        given(redis.setCodeIfAbsent(anyString(), eq(tid), eq(600L)))
                .willReturn(false) // 1번째 실패
                .willReturn(false) // 2번째 실패
                .willReturn(true); // 3번째 성공

        // when
        String code = invokeGenerateCode(tid);

        // then
        assertThat(code).isNotNull();
        assertThat(code).hasSize(16);
        verify(redis, times(3)).setCodeIfAbsent(anyString(), eq(tid), eq(600L));
    }

    @Test
    @DisplayName("10번 모두 실패하면 예외가 발생해야 한다")
    void generateCode_ShouldThrowException_WhenAllAttemptssFail() {
        // given
        String tid = "test-tid-123";
        given(redis.setCodeIfAbsent(anyString(), eq(tid), eq(600L)))
                .willReturn(false); // 모든 시도 실패

        // when & then
        assertThatThrownBy(() -> invokeGenerateCode(tid))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("Code 생성에 실패했습니다");

        verify(redis, times(10)).setCodeIfAbsent(anyString(), eq(tid), eq(600L));
    }

    @Test
    @DisplayName("동일한 TID로 여러 번 호출해도 서로 다른 Code가 생성되어야 한다")
    void generateCode_ShouldGenerateDifferentCodes_ForSameTid() throws Exception {
        // given
        String tid = "test-tid-123";
        given(redis.setCodeIfAbsent(anyString(), eq(tid), eq(600L)))
                .willReturn(true);

        // when
        String code1 = invokeGenerateCode(tid);
        String code2 = invokeGenerateCode(tid);

        // then
        assertThat(code1).isNotEqualTo(code2);
        assertThat(code1).hasSize(16);
        assertThat(code2).hasSize(16);
    }

    @Test
    @DisplayName("생성된 Code는 영문 대소문자와 숫자로만 구성되어야 한다")
    void generateCode_ShouldContainOnlyAlphanumeric() throws Exception {
        // given
        String tid = "test-tid-123";
        given(redis.setCodeIfAbsent(anyString(), eq(tid), eq(600L)))
                .willReturn(true);

        // when
        String code = invokeGenerateCode(tid);

        // then
        assertThat(code).matches("^[A-Za-z0-9]{16}$");
    }

    @Test
    @DisplayName("SETNX 실패 후 성공 시 정확히 해당 Code가 Redis에 저장되어야 한다")
    void generateCode_ShouldSaveCorrectCode_AfterRetry() throws Exception {
        // given
        String tid = "test-tid-123";
        String[] capturedCodes = new String[1];

        given(redis.setCodeIfAbsent(anyString(), eq(tid), eq(600L)))
                .willAnswer(invocation -> {
                    String code = invocation.getArgument(0);
                    if (capturedCodes[0] == null) {
                        capturedCodes[0] = code;
                        return false; // 첫 번째 실패
                    }
                    return true; // 두 번째 성공
                });

        // when
        String generatedCode = invokeGenerateCode(tid);

        // then
        assertThat(generatedCode).isNotNull();
        assertThat(generatedCode).isNotEqualTo(capturedCodes[0]); // 첫 번째와 달라야 함
        verify(redis, times(2)).setCodeIfAbsent(anyString(), eq(tid), eq(600L));
    }

    @Test
    @DisplayName("TTL은 항상 600초(10분)로 설정되어야 한다")
    void generateCode_ShouldSetTTL_To600Seconds() throws Exception {
        // given
        String tid = "test-tid-123";
        given(redis.setCodeIfAbsent(anyString(), eq(tid), eq(600L)))
                .willReturn(true);

        // when
        invokeGenerateCode(tid);

        // then
        verify(redis).setCodeIfAbsent(anyString(), eq(tid), eq(600L)); // TTL 600초 확인
    }

    @Test
    @DisplayName("Redis 예외 발생 시 상위로 전파되어야 한다")
    void generateCode_ShouldPropagateException_WhenRedisThrowsException() {
        // given
        String tid = "test-tid-123";
        given(redis.setCodeIfAbsent(anyString(), eq(tid), eq(600L)))
                .willThrow(new RuntimeException("Redis connection failed"));

        // when & then
        assertThatThrownBy(() -> invokeGenerateCode(tid))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Redis connection failed");
    }
}
