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
import dev.woori.wooriBank.domain.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AuthStoreRedis redis;
    private final BankClientAppRepository bankClientAppRepository;
    private final ValidationUtil validationUtil;

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
        AuthSession session = validationUtil.getSessionOrThrow(request.tid());

        // 검증 성공 후 정보 삭제
        redis.delete(request.tid());

        // 이름 & 계좌번호 return
        return new AccountLookupResDto(session.getName(), session.getAccountNum());
    }
}
