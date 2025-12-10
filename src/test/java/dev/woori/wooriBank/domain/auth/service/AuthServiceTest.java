package dev.woori.wooriBank.domain.auth.service;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.domain.auth.dto.AuthReqDto;
import dev.woori.wooriBank.domain.auth.dto.AuthVerifyReqDto;
import dev.woori.wooriBank.domain.auth.dto.RrnReqDto;
import dev.woori.wooriBank.domain.auth.dto.RrnResDto;
import dev.woori.wooriBank.domain.auth.entity.AuthSession;
import dev.woori.wooriBank.domain.auth.entity.AuthStoreRedis;
import dev.woori.wooriBank.domain.util.EncryptionUtil;
import dev.woori.wooriBank.domain.util.ValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * AuthService 테스트
 * 본인인증 및 인증번호 검증 테스트
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthStoreRedis redis;

    @Mock
    private ValidationUtil validationUtil;

    @Mock
    private EncryptionUtil encryptionUtil;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // maxAttempts 설정
        ReflectionTestUtils.setField(authService, "maxAttempts", 5);
    }

    @Test
    @DisplayName("본인인증 요청 시 개인정보가 암호화되어 세션에 저장되어야 한다")
    void request_ShouldEncryptAndSavePersonalInfo() {
        // given
        String tid = "test-tid-123";
        AuthReqDto request = new AuthReqDto(tid, "김철수", "01012345678", "19900101");

        AuthSession session = AuthSession.builder()
                .tid(tid)
                .build();

        given(validationUtil.getSessionOrThrow(tid)).willReturn(session);
        given(encryptionUtil.encrypt("김철수")).willReturn("encrypted-name");
        given(encryptionUtil.encrypt("01012345678")).willReturn("encrypted-phone");
        given(encryptionUtil.encrypt("19900101")).willReturn("encrypted-birth");

        // when
        authService.request(request);

        // then
        verify(encryptionUtil).encrypt("김철수");
        verify(encryptionUtil).encrypt("01012345678");
        verify(encryptionUtil).encrypt("19900101");
        verify(redis).save(eq(tid), any(AuthSession.class));
    }

    @Test
    @DisplayName("인증번호 검증 성공 시 verified 상태로 변경되어야 한다")
    void verify_ShouldSetVerifiedTrue_WhenAuthCodeMatches() {
        // given
        String tid = "test-tid-123";
        String authCode = "123456";
        AuthVerifyReqDto request = new AuthVerifyReqDto(tid, authCode);

        AuthSession session = AuthSession.builder()
                .tid(tid)
                .authCode(authCode)
                .failedAttempts(0)
                .build();

        given(validationUtil.getSessionOrThrow(tid)).willReturn(session);

        // when
        authService.verify(request);

        // then
        assertThat(session.isVerified()).isTrue();
        assertThat(session.getAuthCode()).isNull(); // 인증번호 삭제
        assertThat(session.getFailedAttempts()).isZero(); // 실패 횟수 초기화
        verify(redis).save(tid, session);
    }

    @Test
    @DisplayName("인증번호 불일치 시 실패 횟수가 증가해야 한다")
    void verify_ShouldIncrementFailedAttempts_WhenAuthCodeDoesNotMatch() {
        // given
        String tid = "test-tid-123";
        AuthVerifyReqDto request = new AuthVerifyReqDto(tid, "wrong-code");

        AuthSession session = AuthSession.builder()
                .tid(tid)
                .authCode("123456")
                .failedAttempts(0)
                .build();

        given(validationUtil.getSessionOrThrow(tid)).willReturn(session);

        // when & then
        assertThatThrownBy(() -> authService.verify(request))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("인증번호가 일치하지 않습니다");

        assertThat(session.getFailedAttempts()).isEqualTo(1);
        verify(redis).save(tid, session);
    }

    @Test
    @DisplayName("최대 실패 횟수 초과 시 인증번호가 만료되어야 한다")
    void verify_ShouldExpireAuthCode_WhenMaxAttemptsExceeded() {
        // given
        String tid = "test-tid-123";
        AuthVerifyReqDto request = new AuthVerifyReqDto(tid, "wrong-code");

        AuthSession session = AuthSession.builder()
                .tid(tid)
                .authCode("123456")
                .failedAttempts(4) // 이미 4번 실패
                .build();

        given(validationUtil.getSessionOrThrow(tid)).willReturn(session);

        // when & then
        assertThatThrownBy(() -> authService.verify(request))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("인증번호를 다시 발급해 주세요");

        assertThat(session.getAuthCode()).isNull(); // 인증번호 만료
        assertThat(session.getFailedAttempts()).isZero(); // 실패 횟수 초기화
        verify(redis).save(tid, session);
    }

    @Test
    @DisplayName("인증번호가 null인 경우 만료 메시지를 반환해야 한다")
    void verify_ShouldThrowException_WhenAuthCodeIsNull() {
        // given
        String tid = "test-tid-123";
        AuthVerifyReqDto request = new AuthVerifyReqDto(tid, "123456");

        AuthSession session = AuthSession.builder()
                .tid(tid)
                .authCode(null) // 인증번호 없음
                .build();

        given(validationUtil.getSessionOrThrow(tid)).willReturn(session);

        // when & then
        assertThatThrownBy(() -> authService.verify(request))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("만료된 인증번호입니다");
    }

    @Test
    @DisplayName("주민등록번호 저장 시 암호화되어 저장되어야 한다")
    void saveRrn_ShouldEncryptAndSaveRrn() {
        // given
        String tid = "test-tid-123";
        String rrn = "9001011234567";
        RrnReqDto request = new RrnReqDto(tid, rrn);

        AuthSession session = AuthSession.builder()
                .tid(tid)
                .verified(true) // 본인인증 완료
                .build();

        given(validationUtil.getSessionOrThrow(tid)).willReturn(session);
        given(encryptionUtil.encrypt(rrn)).willReturn("encrypted-rrn");

        // when
        RrnResDto response = authService.saveRrn(request);

        // then
        assertThat(response.success()).isTrue();
        verify(encryptionUtil).encrypt(rrn);
        verify(redis).save(tid, session);
    }

    @Test
    @DisplayName("본인인증 미완료 상태에서 주민등록번호 저장 시 예외가 발생해야 한다")
    void saveRrn_ShouldThrowException_WhenNotVerified() {
        // given
        String tid = "test-tid-123";
        RrnReqDto request = new RrnReqDto(tid, "9001011234567");

        AuthSession session = AuthSession.builder()
                .tid(tid)
                .verified(false) // 본인인증 미완료
                .build();

        given(validationUtil.getSessionOrThrow(tid)).willReturn(session);

        // when & then
        assertThatThrownBy(() -> authService.saveRrn(request))
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("본인인증을 먼저 완료해주세요");
    }

    @Test
    @DisplayName("인증번호는 6자리 숫자여야 한다")
    void issueNewAuthCode_ShouldGenerate6DigitCode() throws Exception {
        // given
        String tid = "test-tid-123";
        AuthReqDto request = new AuthReqDto(tid, "김철수", "01012345678", "19900101");

        AuthSession session = AuthSession.builder()
                .tid(tid)
                .build();

        given(validationUtil.getSessionOrThrow(tid)).willReturn(session);
        given(encryptionUtil.encrypt(anyString())).willReturn("encrypted");

        // when
        authService.request(request);

        // then
        verify(redis).save(eq(tid), argThat(savedSession -> {
            String authCode = savedSession.getAuthCode();
            return authCode != null &&
                   authCode.matches("\\d{6}") && // 6자리 숫자
                   Integer.parseInt(authCode) >= 0 &&
                   Integer.parseInt(authCode) <= 999999;
        }));
    }

    @Test
    @DisplayName("인증번호 재발급 시 이전 실패 횟수가 초기화되어야 한다")
    void resendAuthCode_ShouldResetFailedAttempts() {
        // given - 첫 번째 요청으로 인증번호 발급
        String tid = "test-tid-123";
        AuthReqDto firstRequest = new AuthReqDto(tid, "김철수", "01012345678", "19900101");

        AuthSession session = AuthSession.builder()
                .tid(tid)
                .failedAttempts(3) // 이미 3번 실패
                .resendAttempts(0)
                .build();

        given(validationUtil.getSessionOrThrow(tid)).willReturn(session);
        given(encryptionUtil.encrypt(anyString())).willReturn("encrypted");

        // when
        authService.request(firstRequest);

        // then
        verify(redis).save(eq(tid), argThat(savedSession ->
                savedSession.getFailedAttempts() == 0 &&
                savedSession.isVerified() == false
        ));
    }
}
