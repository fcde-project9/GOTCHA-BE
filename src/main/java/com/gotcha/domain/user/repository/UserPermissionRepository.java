package com.gotcha.domain.user.repository;

import com.gotcha.domain.user.entity.PermissionType;
import com.gotcha.domain.user.entity.UserPermission;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserPermission Repository
 *
 * 용도:
 * - 사용자의 권한 동의 여부 확인용
 * - 동의 팝업 표시 여부 판단
 */
public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {

    /**
     * 특정 사용자의 특정 권한 조회
     * 사용 예: 위치 기능 사용 전 LOCATION 권한 확인
     */
    Optional<UserPermission> findByUserIdAndPermissionType(Long userId, PermissionType permissionType);

    /**
     * 특정 사용자의 모든 권한 삭제 (회원 탈퇴 시)
     */
    @Modifying
    @Transactional
    void deleteByUserId(Long userId);
}
