package dev.woori.wooriBank.domain.auth.port;


import dev.woori.wooriBank.domain.auth.entity.AuthUsers;

import java.util.Optional;

public interface AuthUserPort {
    Optional<AuthUsers> findByUserId(String userId);

    Boolean existsByUserId(String userId);

    AuthUsers save(AuthUsers authUser);
}
