package dev.woori.wooriBank.config.security;

import dev.woori.wooriBank.config.filter.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    // 인증 없이도 접근 가능한 엔드포인트 목록
    private static final List<String> whiteList = List.of(
            "/auth/login", // 로그인
            "/auth/signup" // 회원가입
    );

    // 관리자만 접근 가능한 엔드포인트 목록
    private static final List<String> adminList = List.of(
            "/admin/*"
    );

    // 개발 모드에서는 인증 적용 x
    @Bean
    @Profile("dev")
    public SecurityFilterChain devFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Profile("!dev")
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf(AbstractHttpConfigurer::disable) // API 테스트용, 실제 서비스면 토큰 기반 CSRF 설정 필요
                .sessionManagement(sessionManagementConfigurer ->
                        sessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 사용 안함
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(whiteList.toArray(new String[0])).permitAll()
                        .requestMatchers(adminList.toArray(new String[0])).hasRole("ADMIN")
                        .anyRequest().authenticated()) // 나머지는 JWT 필요
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
