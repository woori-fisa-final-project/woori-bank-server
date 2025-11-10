package dev.woori.wooriBank.domain.auth.service;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.config.jwt.JwtValidator;
import dev.woori.wooriBank.config.security.Encoder;
import dev.woori.wooriBank.domain.auth.dto.LoginReqDto;
import dev.woori.wooriBank.domain.auth.dto.LoginResDto;
import dev.woori.wooriBank.domain.auth.dto.RefreshReqDto;
import dev.woori.wooriBank.domain.auth.entity.AuthUsers;
import dev.woori.wooriBank.domain.auth.entity.RefreshToken;
import dev.woori.wooriBank.domain.auth.jwt.JwtIssuer;
import dev.woori.wooriBank.domain.auth.port.AuthUserPort;
import dev.woori.wooriBank.domain.auth.port.RefreshTokenPort;
import dev.woori.wooriBank.domain.users.entity.Role;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final AuthUserPort authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final Encoder encoder;
    private final JwtIssuer jwtIssuer;
    private final JwtValidator jwtValidator;
    private final RefreshTokenPort refreshTokenRepository;

    /**
     * id와 pw를 확인 후 사용자임이 확인되면 jwt 토큰을 발급합니다.
     * @param loginReqDto 로그인 입력값 - id / pw
     * @return loginResDto - access token / refresh token
     */
    public LoginResDto login(LoginReqDto loginReqDto) {
        AuthUsers user = authUserRepository.findByUserId(loginReqDto.userId())
                .orElseThrow(() -> new CommonException(ErrorCode.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."));

        if(!passwordEncoder.matches(loginReqDto.password(), user.getPassword())){
            throw new CommonException(ErrorCode.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        return generateAndSaveToken(loginReqDto.userId(), user.getRole());
    }

    /**
     * 입력된 refresh token을 이용해 새 access token을 발급합니다.
     * @param refreshReqDto 사용자의 refresh token
     * @return access token
     */
    public LoginResDto refresh(RefreshReqDto refreshReqDto) {
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

    /**
     * 사용자의 요청을 받아서 로그아웃 처리 - db에서 refresh token 삭제
     * @param username 사용자 id
     * @return 결과 메시지
     */
    public String logout(String username) {
        refreshTokenRepository.deleteByUsername(username);
        return "로그아웃되었습니다.";
    }

    public LoginResDto generateAndSaveToken(String username, Role role){
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

        return new LoginResDto(accessToken, refreshToken);
    }
}
