package dev.woori.wooriBank.domain.auth.entity;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

// redis 저장소
@Component
@RequiredArgsConstructor
public class AuthStoreRedis {
    private final RedisTemplate<String, Object> redisTemplate;
    @Value("${auth.session.ttl}")
    private long sessionTtl;

    // 세션 id를 키값으로 session 객체를 저장
    public void save(String sessionId, AuthSession session) {
        redisTemplate.opsForValue().set(sessionId, session, sessionTtl, TimeUnit.SECONDS);
    }

    // id에 매핑된 객체를 불러옴
    public AuthSession get(String sessionId) {
        Object obj = redisTemplate.opsForValue().get(sessionId);
        return (obj instanceof AuthSession session) ? session : null;
    }

    public void delete(String sessionId) {
        redisTemplate.delete(sessionId);
    }

    /**
     * Code를 키로 TID 저장 (일회용 Code 매핑)
     */
    public void saveCode(String code, String tid, long ttl) {
        String key = "code:" + code;
        redisTemplate.opsForValue().set(key, tid, ttl, TimeUnit.SECONDS);
    }

    /**
     * Code로 TID 조회
     */
    public String getTidByCode(String code) {
        String key = "code:" + code;
        Object obj = redisTemplate.opsForValue().get(key);
        return (obj instanceof String tid) ? tid : null;
    }

    /**
     * Code 삭제 (일회용 사용 후)
     */
    public void deleteCode(String code) {
        String key = "code:" + code;
        redisTemplate.delete(key);
    }
}
