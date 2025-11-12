package dev.woori.wooriBank.config.filter;

import dev.woori.wooriBank.domain.auth.entity.BankClientApp;
import dev.woori.wooriBank.domain.auth.repository.BankClientAppRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppKeySecretFilter extends OncePerRequestFilter {

    private final BankClientAppRepository bankClientAppRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 토큰 발급 요청인지 확인 - 아니면 다음 필터로 넘어감
        String path = request.getRequestURI();
        if (!path.equals("/auth/token")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 토큰 발급 요청인 경우
        // 헤더에서 appKey와 secretKey값을 꺼내옴
        String appKey = request.getHeader("appKey");
        String secretKey = request.getHeader("secretKey");

        if (appKey == null || secretKey == null) {
            log.warn("[Unauthorized] {} {} - {}", request.getMethod(), request.getRequestURI(),
                    "appKey 혹은 secretKey가 존재하지 않습니다.");

            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("utf-8");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());

            response.getWriter().write("{\"message\":\"appKey 혹은 secretKey가 존재하지 않습니다.\"}");
            return;
        }

        // DB에서 검증
        BankClientApp clientApp = bankClientAppRepository.findByAppKey(appKey).orElse(null);
        if(clientApp == null || !clientApp.getSecretKey().equals(secretKey)) {
            log.warn("[Unauthorized] {} {} - {}", request.getMethod(), request.getRequestURI(),
                    "appKey 혹은 secretKey가 유효하지 않습니다.");

            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("utf-8");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());

            response.getWriter().write("{\"message\":\"appKey 혹은 secretKey가 유효하지 않습니다.\"}");
            return;
        }
        // TODO: payload secretKey로 검증하기

        // 검증이 끝나면 clientApp을 attribute로 설정
        request.setAttribute("clientApp", clientApp);

        filterChain.doFilter(request, response);
    }
}
