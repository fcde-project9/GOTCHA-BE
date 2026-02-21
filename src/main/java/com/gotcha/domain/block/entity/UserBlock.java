package com.gotcha.domain.block.entity;

import com.gotcha._global.entity.BaseTimeEntity;
import com.gotcha.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_blocks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBlock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    @Builder
    public UserBlock(User blocker, User blocked) {
        this.blocker = blocker;
        this.blocked = blocked;
    }
}
