package com.gotcha.domain.user.entity;

import com.gotcha._global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 권한 동의 상태 (현재 상태만 저장)
 *
 * 용도:
 * - DB에서 조회하여 사용자의 권한 동의 여부 확인
 * - 미동의 시 프론트엔드에서 동의 팝업 표시
 *
 * 중복 방지:
 * - UNIQUE(user_id, permission_type)로 한 사용자는 각 권한별 1개만 가능
 */
@Entity
@Table(
    name = "user_permissions",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "permission_type"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPermission extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_type", nullable = false, length = 20)
    private PermissionType permissionType;

    @Column(nullable = false)
    private Boolean isAgreed;

    @Column
    private LocalDateTime agreedAt;

    @Builder
    public UserPermission(User user, PermissionType permissionType, Boolean isAgreed) {
        this.user = user;
        this.permissionType = permissionType;
        this.isAgreed = isAgreed != null ? isAgreed : false;
        this.agreedAt = this.isAgreed ? LocalDateTime.now() : null;
    }

    public void updateAgreement(Boolean isAgreed) {
        this.isAgreed = isAgreed;
        this.agreedAt = isAgreed ? LocalDateTime.now() : null;
    }
}
