package dev.woori.wooriBank.domain.users.repository;

import dev.woori.wooriBank.domain.users.entity.BankUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankUserRepository extends JpaRepository<BankUser, Long> {
    /**
     * RRN으로 사용자 조회 (DB 암호화 이전 코드 - 사용 비권장)
     * @deprecated DB에 암호화되어 저장되므로 검색 불가, findByRrnHash 사용
     */
    @Deprecated
    Optional<BankUser> findByRrn(String rrn);

    /**
     * RRN 해시값으로 사용자 조회
     * DB에 암호화된 RRN 검색을 위해 해시값 사용
     */
    Optional<BankUser> findByRrnHash(String rrnHash);
}
