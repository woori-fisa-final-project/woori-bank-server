package dev.woori.wooriBank.domain.auth.service;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.config.jwt.JwtInfo;
import dev.woori.wooriBank.config.jwt.JwtValidator;
import dev.woori.wooriBank.config.security.Encoder;
import dev.woori.wooriBank.domain.auth.dto.*;
import dev.woori.wooriBank.domain.auth.entity.AuthSession;
import dev.woori.wooriBank.domain.auth.entity.AuthStoreRedis;
import dev.woori.wooriBank.domain.auth.entity.RefreshToken;
import dev.woori.wooriBank.domain.auth.jwt.JwtIssuer;
import dev.woori.wooriBank.domain.auth.port.RefreshTokenPort;
import dev.woori.wooriBank.domain.auth.entity.Role;
import dev.woori.wooriBank.domain.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final Encoder encoder;
    private final JwtIssuer jwtIssuer;
    private final JwtValidator jwtValidator;
    private final RefreshTokenPort refreshTokenRepository;
    private final AuthStoreRedis redis;
    private final ValidationUtil validationUtil;
    private static final SecureRandom random = new SecureRandom();
    @Value("${auth.verification.max-attempts}")
    private int maxAttempts;

    /**
     * name에 따른 token을 발급합니다.
     * @param name token에 들어가는 이름
     * @return access token + refresh token
     */
    public TokenResDto issueToken(String name){
        return generateAndSaveToken(name, Role.ROLE_USER);
    }

    /**
     * 입력된 refresh token을 이용해 새 access token을 발급합니다.
     * @param refreshReqDto 사용자의 refresh token
     * @return access token
     */
    public TokenResDto refresh(RefreshReqDto refreshReqDto) {
        String refreshToken = refreshReqDto.refreshToken();

        // 토큰 만료 및 유효성 검증
        JwtInfo jwtInfo = jwtValidator.parseToken(refreshToken);
        String username = jwtInfo.username();
        Role role = jwtInfo.role();

        // 토큰 존재 여부 검증
        RefreshToken token = refreshTokenRepository.findByUsername(username)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "토큰이 존재하지 않습니다."));

        // 토큰 일치 여부 검증
        if(!encoder.matches(refreshReqDto.refreshToken(), token.getToken())){
            throw new CommonException(ErrorCode.UNAUTHORIZED, "토큰이 일치하지 않습니다.");
        }

        // 검증 끝나면 access token/refresh token 생성해서 return
        return generateAndSaveToken(username, role);
    }

    /**
     * 사용자가 입력한 본인인증 정보를 저장하고, 인증번호를 생성합니다.
     * @param request tid + 사용자가 입력한 개인정보(이름, 생일, 전화번호)
     */
    public void request(AuthReqDto request){
        // tid 검증
        AuthSession session = validationUtil.getSessionOrThrow(request.tid());

        // 세션에 개인정보 임시저장
        session.setName(request.name());
        session.setBirth(request.birth());
        session.setPhone(request.phone());

        // 인증번호 발급
        issueNewAuthCode(session);
    }

    /**
     * 사용자가 입력한 인증번호를 검증합니다.
     * @param request tid + 입력한 인증번호
     */
    public void verify(AuthVerifyReqDto request){
        // tid 검증
        AuthSession session = validationUtil.getSessionOrThrow(request.tid());

        // 인증번호 값이 null일 경우
        if (session.getAuthCode() == null) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "만료된 인증번호입니다. 재발송 버튼을 눌러주세요.");
        }

        // 인증에 실패했을 경우
        if(!request.authCode().equals(session.getAuthCode())){
            // 일정 횟수 이상 실패했을 경우
            if(session.getFailedAttempts() >= maxAttempts){
                session.setAuthCode(null);
                session.setFailedAttempts(0);
                redis.save(request.tid(), session);
                throw new CommonException(ErrorCode.FORBIDDEN, "인증번호 검증에 실패했습니다. 인증번호를 다시 발급해 주세요.");
            }
            session.setFailedAttempts(session.getFailedAttempts() + 1);
            redis.save(request.tid(), session); // 실패 횟수 업데이트
            throw new CommonException(ErrorCode.UNAUTHORIZED, "인증번호가 일치하지 않습니다.");
        }

        // 인증에 성공하면 verified 상태로 전환
        session.setVerified(true);
        session.setAuthCode(null);
        session.setFailedAttempts(0);
        redis.save(request.tid(), session);
    }

    /**
     * 인증번호 재발급 api
     * @param request tid
     */
    public void resendAuthCode(AuthCodeRefreshReqDto request){
        AuthSession session = validationUtil.getSessionOrThrow(request.tid());
        issueNewAuthCode(session);
    }

    private TokenResDto generateAndSaveToken(String username, Role role){
        // jwt 토큰 저장 로직
        String accessToken = jwtIssuer.generateAccessToken(username, role);
        var refreshTokenInfo = jwtIssuer.generateRefreshToken(username, role);
        String refreshToken = refreshTokenInfo.token();
        Instant refreshTokenExpiration = refreshTokenInfo.expiration();

        // 이전 토큰이 있다면 유효기간 갱신
        // 없다면 만들어서 저장
        RefreshToken token = refreshTokenRepository.findByUsername(username)
                .map(entity -> {
                    entity.updateToken(encoder.encode(refreshToken), refreshTokenExpiration);
                    return entity;
                })
                .orElseGet(() -> RefreshToken.builder()
                        .username(username)
                        .token(encoder.encode(refreshToken))
                        .expiration(refreshTokenExpiration)
                        .build());
        refreshTokenRepository.save(token);

        return new TokenResDto(accessToken, refreshToken);
    }

    // 인증번호 발급용 메서드
    private void issueNewAuthCode(AuthSession session){
        // 재발급 시도가 너무 많을 경우
        if(session.getResendAttempts() >= maxAttempts){
            redis.delete(session.getTid()); // 세션 삭제
            throw new CommonException(ErrorCode.FORBIDDEN,
                    "재발송 횟수를 초과했습니다. 처음부터 다시 시도해주세요.");
        }

        // 새로운 인증번호 발급
        String newCode = String.format("%06d", random.nextInt(1000000));
        session.setAuthCode(newCode);

        session.setFailedAttempts(0);
        session.setVerified(false);
        session.setResendAttempts(session.getResendAttempts() + 1);

        // 테스트용: 만들어진 코드를 콘솔에서 확인할 수 있도록 설정
        log.info("authCode: {}", newCode);
        redis.save(session.getTid(), session);
    }
}
