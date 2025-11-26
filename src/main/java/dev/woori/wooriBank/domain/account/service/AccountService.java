package dev.woori.wooriBank.domain.account.service;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.domain.account.dto.AccountLookupReqDto;
import dev.woori.wooriBank.domain.account.dto.AccountLookupResDto;
import dev.woori.wooriBank.domain.account.dto.TidResDto;
import dev.woori.wooriBank.domain.auth.entity.AuthSession;
import dev.woori.wooriBank.domain.auth.entity.AuthStoreRedis;
import dev.woori.wooriBank.domain.auth.repository.BankClientAppRepository;
import dev.woori.wooriBank.domain.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AuthStoreRedis redis;
    private final ValidationUtil validationUtil;

    public TidResDto getTid(String clientId) {

        // 랜덤 tid 생성 및 저장
        String tid = UUID.randomUUID().toString();

        AuthSession session = AuthSession.builder()
                .tid(tid)
                .clientId(clientId)
                .build();

        // redis 저장
        redis.save(tid, session);

        return new TidResDto(tid);
    }

    public AccountLookupResDto accountLookup(AccountLookupReqDto request){
        AuthSession session = validationUtil.getSessionOrThrow(request.tid());

        // 인증 여부 확인
        if (!session.isVerified()) {
            throw new CommonException(ErrorCode.FORBIDDEN, "본인인증이 완료되지 않았습니다.");
        }

        // 검증 성공 후 정보 삭제
        redis.delete(request.tid());

        // 이름 & 계좌번호 return
        return new AccountLookupResDto(session.getName(), session.getAccountNum());
    }
}
