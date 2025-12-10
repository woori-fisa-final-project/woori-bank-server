package dev.woori.wooriBank.domain.util;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.domain.auth.entity.AuthSession;
import dev.woori.wooriBank.domain.auth.entity.AuthStoreRedis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 세션 검증 유틸리티
 *
 * 보안 정책:
 * - Redis: 민감 정보는 암호화되어 저장됨 (AuthService에서 암호화)
 * - DB: JPA AttributeConverter가 자동으로 암호화/복호화 처리
 * - 이 클래스는 복호화를 수행하지 않음 (각 계층에서 자동 처리)
 */
@Component
@RequiredArgsConstructor
public class ValidationUtil {
    private final AuthStoreRedis redis;

    /**
     * 세션 조회 (암호화된 채로 반환)
     * Redis에 저장된 민감 정보는 암호화되어 있으며,
     * 필요 시 사용처에서 복호화하거나 DB 저장 시 JPA Converter가 처리합니다.
     */
    public AuthSession getSessionOrThrow(String tid) {
        AuthSession session = redis.get(tid);
        if (session == null) {
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                    "세션이 만료되었습니다. 처음부터 다시 시작해주세요.");
        }
        return session;
    }
}
