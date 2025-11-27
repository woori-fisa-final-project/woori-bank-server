package dev.woori.wooriBank.domain.util;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.domain.auth.entity.AuthSession;
import dev.woori.wooriBank.domain.auth.entity.AuthStoreRedis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidationUtil {
    private final AuthStoreRedis redis;

    public AuthSession getSessionOrThrow(String tid) {
        AuthSession session = redis.get(tid);
        if (session == null) {
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                    "세션이 만료되었습니다. 처음부터 다시 시작해주세요.");
        }
        return session;
    }
}
