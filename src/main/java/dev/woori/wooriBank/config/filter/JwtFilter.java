package dev.woori.wooriBank.config.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.jwt.JwtInfo;
import dev.woori.wooriBank.config.jwt.JwtValidator;
import dev.woori.wooriBank.config.response.BaseResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtValidator jwtValidator;
    private final String BEARER = "Bearer ";
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 헤더에서 키값 가져오기
        String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        log.debug("Authorization Header is present: {}", accessToken != null && accessToken.startsWith(BEARER));

        try{
            if (accessToken != null && accessToken.startsWith(BEARER)) {
                String token = accessToken.substring(BEARER.length()); // 순수 토큰값만 가져오기

                JwtInfo jwtInfo = jwtValidator.parseToken(token);

                // Authentication 객체 생성
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(jwtInfo.username(), null,
                                List.of(new SimpleGrantedAuthority(jwtInfo.role().name())));

                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("Authentication after JWT filter = {}",
                        SecurityContextHolder.getContext().getAuthentication());
            }

            filterChain.doFilter(request, response);
        }catch (CommonException e){
            log.warn("{} - {}", e.getErrorCode(), e.getMessage());
            response.setStatus(e.getErrorCode().getStatus().value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    BaseResponse.of(e.getErrorCode())
            ));
        }
    }
}
