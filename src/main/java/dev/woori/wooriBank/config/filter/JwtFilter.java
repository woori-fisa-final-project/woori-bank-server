package dev.woori.wooriBank.config.filter;

import dev.woori.wooriBank.config.jwt.JwtInfo;
import dev.woori.wooriBank.config.jwt.JwtValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtValidator jwtValidator;
    private final String BEARER = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 헤더에서 키값 가져오기
        String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (accessToken != null && accessToken.startsWith(BEARER)) {
            String token = accessToken.substring(BEARER.length()); // 순수 토큰값만 가져오기
            JwtInfo jwtInfo = jwtValidator.parseToken(token);

            // Authentication 객체 생성
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(jwtInfo.username(), null,
                            List.of(new SimpleGrantedAuthority(jwtInfo.role().name())));

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
