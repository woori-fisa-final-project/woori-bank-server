package dev.woori.wooriBank.domain.account.service;

import dev.woori.wooriBank.config.exception.CommonException;
import dev.woori.wooriBank.config.exception.ErrorCode;
import dev.woori.wooriBank.domain.account.dto.AccountLookupReqDto;
import dev.woori.wooriBank.domain.account.dto.AccountLookupResDto;
import dev.woori.wooriBank.domain.auth.entity.AuthSession;
import dev.woori.wooriBank.domain.auth.entity.AuthStoreRedis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AuthStoreRedis redis;

    public AccountLookupResDto accountLookup(AccountLookupReqDto request){
        String id = request.id();
        String code = request.code();

        // 코드와 redis 내부 저장소에 저장된 코드를 비교하기
        AuthSession session = redis.get(id);

        // 시간만료 등의 이유로 데이터가 사라진 경우
        if(session == null){
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND, "데이터를 찾을 수 없습니다.");
        }
        // 코드 검증이 실패할 경우
        if(!code.equals(session.getCode())){
            throw new CommonException(ErrorCode.UNAUTHORIZED, "인증 코드 오류");
        }

        // 검증 성공 후 정보 삭제
        redis.delete(id);

        // 이름 & 계좌번호 return
        return new AccountLookupResDto(session.getName(), session.getAccountNum());
    }
}
