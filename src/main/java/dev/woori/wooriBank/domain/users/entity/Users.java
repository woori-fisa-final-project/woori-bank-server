package dev.woori.wooriBank.domain.users.entity;

import dev.woori.wooriBank.config.BaseEntity;
import dev.woori.wooriBank.domain.auth.entity.AuthUsers;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Users extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_user_id")
    private AuthUsers authUser;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private Integer points;

}
