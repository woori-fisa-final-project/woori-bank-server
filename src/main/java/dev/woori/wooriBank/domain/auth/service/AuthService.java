package dev.woori.wooriBank.domain.auth.service;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.config.jwt.JwtInfo;
import dev.woori.wooriBank.config.jwt.JwtValidator;
import dev.woori.wooriBank.config.security.Encoder;
import dev.woori.wooriBank.domain.auth.dto.*;
import dev.woori.wooriBank.domain.auth.entity.AuthStoreRedis;
import dev.woori.wooriBank.domain.auth.entity.RefreshToken;
import dev.woori.wooriBank.domain.auth.jwt.JwtIssuer;
import dev.woori.wooriBank.domain.auth.port.RefreshTokenPort;
import dev.woori.wooriBank.domain.auth.entity.Role;
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
     * 사용자의 요청을 받아 개인정보를 임시로 저장하고 인증번호를 생성합니다.
     * @param userId 사용자 id (혹은 redis 키값 / sessionId 같은 개념)
     * @param request 사용자의 개인정보가 담긴 dto
     */
    public void request(String userId, AuthReqDto request){
        // 인증번호 생성 - 랜덤 6자리
        String authCode = String.format("%06d", random.nextInt(1000000));

        // 사용자 정보 임시저장
        AuthSession session = AuthSession.builder()
                .id(userId)
                .name(request.name())
                .phone(request.phone())
                .birth(request.birth())
                .authCode(authCode)
                .failedAttempts(0)
                .verified(false)
                .build();

        redis.save(userId, session);

        // 테스트용: 만들어진 코드를 콘솔에서 확인할 수 있도록 설정
        log.debug("authCode: {}", authCode);
    }

    /**
     * 입력받은 인증번호가 올바른지 검증합니다.
     * @param userId 사용자 id (혹은 redis 키값 / sessionId 같은 개념)
     * @param request 사용자가 입력한 인증번호
     */
    public void verify(String userId, AuthVerifyReqDto request){
        // 입력받은 인증번호와 저장된 인증번호 비교
        AuthSession session = redis.get(userId);

        // 시간만료 등의 이유로 데이터가 사라진 경우
        if(session == null){
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND, "데이터를 찾을 수 없습니다.");
        }

        if(!request.authCode().equals(session.getAuthCode())){
            // 일정 횟수 이상 실패했을 경우
            if(session.getFailedAttempts() >= maxAttempts){
                redis.delete(userId); // 세션 삭제
                throw new CommonException(ErrorCode.FORBIDDEN, "인증번호 검증에 실패했습니다. 인증번호를 다시 발급해 주세요.");
            }
            session.setFailedAttempts(session.getFailedAttempts() + 1);
            redis.save(userId, session); // 실패 횟수 업데이트
            throw new CommonException(ErrorCode.UNAUTHORIZED, "인증번호가 일치하지 않습니다.");
        }

        // 인증에 성공하면 verified 상태로 전환
        session.setVerified(true);
        redis.save(userId, session);
    }

    public TokenResDto generateAndSaveToken(String username, Role role){
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
}
