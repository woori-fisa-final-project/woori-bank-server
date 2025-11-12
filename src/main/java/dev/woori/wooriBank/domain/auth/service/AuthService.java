package dev.woori.wooriBank.domain.auth.service;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.config.jwt.JwtValidator;
import dev.woori.wooriBank.config.security.Encoder;
import dev.woori.wooriBank.domain.auth.dto.TokenResDto;
import dev.woori.wooriBank.domain.auth.dto.RefreshReqDto;
import dev.woori.wooriBank.domain.auth.entity.BankClientApp;
import dev.woori.wooriBank.domain.auth.entity.RefreshToken;
import dev.woori.wooriBank.domain.auth.jwt.JwtIssuer;
import dev.woori.wooriBank.domain.auth.port.RefreshTokenPort;
import dev.woori.wooriBank.domain.auth.entity.Role;
import dev.woori.wooriBank.domain.auth.repository.BankClientAppRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public final BankClientAppRepository bankClientAppRepository;

    /**
     * appKey와 secretKey를 받고 검증해 access token과 refresh token을 발급합니다.
     * @param appKey api에 접근하기 위한 key
     * @param secretKey 개별 사용자마다 주어지는 비밀키 (추후 payload 암호화에 사용)
     * @return access token + refresh token
     */
    public TokenResDto issueToken(String appKey, String secretKey){
        BankClientApp clientApp = bankClientAppRepository.findByAppKey(appKey)
                .orElseThrow(() -> new CommonException(ErrorCode.UNAUTHORIZED, "Invalid appKey"));

        if (!clientApp.getSecretKey().equals(secretKey)) {
            throw new CommonException(ErrorCode.UNAUTHORIZED, "Invalid secretKey");
        }

        return generateAndSaveToken(appKey, Role.ROLE_USER);
    }

    /**
     * 입력된 refresh token을 이용해 새 access token을 발급합니다.
     * @param refreshReqDto 사용자의 refresh token
     * @return access token
     */
    public TokenResDto refresh(RefreshReqDto refreshReqDto) {
        String refreshToken = refreshReqDto.refreshToken();
        String username;
        Role role;

        // 토큰 만료 및 유효성 검증
        try {
            username = jwtValidator.getUsername(refreshToken);
            role = jwtValidator.getRole(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new CommonException(ErrorCode.TOKEN_EXPIRED, "토큰이 만료되었습니다.");
        } catch (JwtException | IllegalArgumentException e) {
            throw new CommonException(ErrorCode.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
        }

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
