package dev.woori.wooriBank.domain.util;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.domain.auth.entity.AuthSession;
import dev.woori.wooriBank.domain.auth.entity.AuthStoreRedis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * ValidationUtil 테스트
 * 세션 조회 및 민감 정보 복호화 검증
 */
@ExtendWith(MockitoExtension.class)
class ValidationUtilTest {

    @Mock
    private AuthStoreRedis redis;

    @Mock
    private EncryptionUtil encryptionUtil;

    @InjectMocks
    private ValidationUtil validationUtil;

    @Test
    @DisplayName("세션 조회 성공 시 민감 정보가 복호화되어야 한다")
    void getSessionOrThrow_ShouldDecryptSensitiveInfo_WhenSessionExists() {
        // given
        String tid = "test-tid-123";
        AuthSession encryptedSession = AuthSession.builder()
                .tid(tid)
                .name("encrypted-name")
                .birth("encrypted-birth")
                .phone("encrypted-phone")
                .rrn("encrypted-rrn")
                .build();

        given(redis.get(tid)).willReturn(encryptedSession);
        given(encryptionUtil.decrypt("encrypted-name")).willReturn("김철수");
        given(encryptionUtil.decrypt("encrypted-birth")).willReturn("19900101");
        given(encryptionUtil.decrypt("encrypted-phone")).willReturn("01012345678");
        given(encryptionUtil.decrypt("encrypted-rrn")).willReturn("9001011234567");

        // when
        AuthSession session = validationUtil.getSessionOrThrow(tid);

        // then
        assertThat(session.getName()).isEqualTo("김철수");
        assertThat(session.getBirth()).isEqualTo("19900101");
        assertThat(session.getPhone()).isEqualTo("01012345678");
        assertThat(session.getRrn()).isEqualTo("9001011234567");

        verify(encryptionUtil).decrypt("encrypted-name");
        verify(encryptionUtil).decrypt("encrypted-birth");
        verify(encryptionUtil).decrypt("encrypted-phone");
        verify(encryptionUtil).decrypt("encrypted-rrn");
    }

    @Test
    @DisplayName("세션이 존재하지 않으면 예외가 발생해야 한다")
    void getSessionOrThrow_ShouldThrowException_WhenSessionNotFound() {
        // given
        String tid = "non-existent-tid";
        given(redis.get(tid)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> validationUtil.getSessionOrThrow(tid))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("세션이 만료되었습니다");
    }

    @Test
    @DisplayName("일부 필드만 null이면 해당 필드는 복호화하지 않아야 한다")
    void getSessionOrThrow_ShouldSkipDecryption_WhenFieldIsNull() {
        // given
        String tid = "test-tid-123";
        AuthSession session = AuthSession.builder()
                .tid(tid)
                .name("encrypted-name")
                .birth(null) // null
                .phone("encrypted-phone")
                .rrn(null) // null
                .build();

        given(redis.get(tid)).willReturn(session);
        given(encryptionUtil.decrypt("encrypted-name")).willReturn("김철수");
        given(encryptionUtil.decrypt("encrypted-phone")).willReturn("01012345678");

        // when
        AuthSession result = validationUtil.getSessionOrThrow(tid);

        // then
        assertThat(result.getName()).isEqualTo("김철수");
        assertThat(result.getBirth()).isNull();
        assertThat(result.getPhone()).isEqualTo("01012345678");
        assertThat(result.getRrn()).isNull();

        verify(encryptionUtil).decrypt("encrypted-name");
        verify(encryptionUtil).decrypt("encrypted-phone");
        verify(encryptionUtil, never()).decrypt(isNull()); // null은 복호화하지 않음
    }

    @Test
    @DisplayName("모든 민감 정보가 null이어도 세션은 정상 반환되어야 한다")
    void getSessionOrThrow_ShouldReturnSession_WhenAllSensitiveFieldsAreNull() {
        // given
        String tid = "test-tid-123";
        AuthSession session = AuthSession.builder()
                .tid(tid)
                .name(null)
                .birth(null)
                .phone(null)
                .rrn(null)
                .build();

        given(redis.get(tid)).willReturn(session);

        // when
        AuthSession result = validationUtil.getSessionOrThrow(tid);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTid()).isEqualTo(tid);
        verify(encryptionUtil, never()).decrypt(anyString());
    }

    @Test
    @DisplayName("복호화 실패 시 예외가 전파되어야 한다")
    void getSessionOrThrow_ShouldPropagateException_WhenDecryptionFails() {
        // given
        String tid = "test-tid-123";
        AuthSession session = AuthSession.builder()
                .tid(tid)
                .name("encrypted-name")
                .build();

        given(redis.get(tid)).willReturn(session);
        given(encryptionUtil.decrypt("encrypted-name"))
                .willThrow(new RuntimeException("복호화 중 오류가 발생했습니다"));

        // when & then
        assertThatThrownBy(() -> validationUtil.getSessionOrThrow(tid))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("복호화 중 오류가 발생했습니다");
    }

    @Test
    @DisplayName("Redis에서 가져온 세션 객체를 직접 수정해도 원본은 변경되지 않아야 한다")
    void getSessionOrThrow_ShouldReturnSameSessionInstance() {
        // given
        String tid = "test-tid-123";
        AuthSession originalSession = AuthSession.builder()
                .tid(tid)
                .name("encrypted-name")
                .build();

        given(redis.get(tid)).willReturn(originalSession);
        given(encryptionUtil.decrypt("encrypted-name")).willReturn("김철수");

        // when
        AuthSession session = validationUtil.getSessionOrThrow(tid);

        // then
        assertThat(session).isSameAs(originalSession); // 동일한 인스턴스
        assertThat(session.getName()).isEqualTo("김철수"); // 복호화된 값으로 변경됨
    }

    @Test
    @DisplayName("세션 조회 후 복호화는 한 번만 수행되어야 한다")
    void getSessionOrThrow_ShouldDecryptOnlyOnce() {
        // given
        String tid = "test-tid-123";
        AuthSession session = AuthSession.builder()
                .tid(tid)
                .name("encrypted-name")
                .phone("encrypted-phone")
                .build();

        given(redis.get(tid)).willReturn(session);
        given(encryptionUtil.decrypt("encrypted-name")).willReturn("김철수");
        given(encryptionUtil.decrypt("encrypted-phone")).willReturn("01012345678");

        // when
        validationUtil.getSessionOrThrow(tid);

        // then
        verify(encryptionUtil, times(1)).decrypt("encrypted-name");
        verify(encryptionUtil, times(1)).decrypt("encrypted-phone");
    }
}
