package dev.woori.wooriBank.domain.auth.entity;

import dev.woori.wooriBank.domain.auth.dto.AuthSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

// redis 저장소
@Component
@RequiredArgsConstructor
public class AuthStoreRedis {
    private final RedisTemplate<String, Object> redisTemplate;

    // 세션 id를 키값으로 session 객체를 저장
    public void save(String sessionId, AuthSession session) {
        redisTemplate.opsForValue().set(sessionId, session, 5, TimeUnit.MINUTES); // 만료 5분
    }

    // id에 매핑된 객체를 불러옴
    public AuthSession get(String sessionId) {
        Object obj = redisTemplate.opsForValue().get(sessionId);
        return (obj instanceof AuthSession) ? (AuthSession) obj : null;
    }

    public void delete(String sessionId) {
        redisTemplate.delete(sessionId);
    }
}
