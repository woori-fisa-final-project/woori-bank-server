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

    /**
     * Code를 원자적으로 저장 (SETNX - SET if Not eXists)
     * 동시성 제어를 위해 존재하지 않는 경우에만 저장
     *
     * @param code 저장할 코드
     * @param tid  매핑할 TID
     * @param ttl  만료 시간 (초)
     * @return 저장 성공 여부 (true: 성공, false: 이미 존재)
     */
    public boolean setCodeIfAbsent(String code, String tid, long ttl) {
        String key = "code:" + code;
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, tid, ttl, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }
}
