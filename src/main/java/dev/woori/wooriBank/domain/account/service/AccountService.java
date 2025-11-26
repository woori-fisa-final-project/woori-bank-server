package dev.woori.wooriBank.domain.account.service;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.domain.account.dto.AccountLookupReqDto;
import dev.woori.wooriBank.domain.account.dto.AccountLookupResDto;
import dev.woori.wooriBank.domain.account.dto.TidReqDto;
import dev.woori.wooriBank.domain.account.dto.TidResDto;
import dev.woori.wooriBank.domain.auth.entity.AuthSession;
import dev.woori.wooriBank.domain.auth.entity.AuthStoreRedis;
import dev.woori.wooriBank.domain.auth.repository.BankClientAppRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AuthStoreRedis redis;
    private final BankClientAppRepository bankClientAppRepository;

    public TidResDto getTid(TidReqDto tidReqDto) {
        // clientId 검증
        if(!bankClientAppRepository.existsByClientId(tidReqDto.clientId())){
            throw new CommonException(ErrorCode.FORBIDDEN);
        }

        // 랜덤 tid 생성 및 저장
        String tid = UUID.randomUUID().toString();

        AuthSession session = AuthSession.builder()
                .clientId(tidReqDto.clientId())
                .build();

        // redis 저장
        redis.save(tid, session);

        return new TidResDto(tid);
    }

    public AccountLookupResDto accountLookup(AccountLookupReqDto request){
        AuthSession session = getSessionOrThrow(request.tid());

        // 검증 성공 후 정보 삭제
        redis.delete(request.tid());

        // 이름 & 계좌번호 return
        return new AccountLookupResDto(session.getName(), session.getAccountNum());
    }

    private AuthSession getSessionOrThrow(String userId) {
        AuthSession session = redis.get(userId);
        if (session == null) {
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND,
                    "세션이 만료되었습니다. 처음부터 다시 시작해주세요.");
        }
        return session;
    }
}
