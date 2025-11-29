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
    private final EncryptionUtil encryptionUtil;

    /**
     * 세션 조회 및 민감 정보 복호화
     */
    public AuthSession getSessionOrThrow(String tid) {
        AuthSession session = redis.get(tid);
        if (session == null) {
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                    "세션이 만료되었습니다. 처음부터 다시 시작해주세요.");
        }

        // 민감 정보 복호화
        if (session.getName() != null) {
            session.setName(encryptionUtil.decrypt(session.getName()));
        }
        if (session.getBirth() != null) {
            session.setBirth(encryptionUtil.decrypt(session.getBirth()));
        }
        if (session.getPhone() != null) {
            session.setPhone(encryptionUtil.decrypt(session.getPhone()));
        }
        if (session.getRrn() != null) {
            session.setRrn(encryptionUtil.decrypt(session.getRrn()));
        }

        return session;
    }
}
