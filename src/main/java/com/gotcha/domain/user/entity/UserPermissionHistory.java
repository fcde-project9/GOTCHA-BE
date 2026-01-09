package com.gotcha.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 권한 변경 이력 (법적 증빙용)
 *
 * 목적:
 * - 권한 동의/거부 변경 이력을 DB에 저장 (법적 증빙, 감사 로그)
 * - 애플리케이션에서는 save()로 저장만 함
 * - 조회는 필요시 DB에서 직접 확인
 */
@Entity
@Table(name = "user_permission_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPermissionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_type", nullable = false, length = 20)
    private PermissionType permissionType;

    @Column(nullable = false)
    private Boolean isAgreed;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    @Column(length = 200)
    private String deviceInfo;

    @Builder
    public UserPermissionHistory(Long userId, PermissionType permissionType, Boolean isAgreed,
                                 String deviceInfo) {
        this.userId = userId;
        this.permissionType = permissionType;
        this.isAgreed = isAgreed;
        this.changedAt = LocalDateTime.now();
        this.deviceInfo = deviceInfo;
    }
}
