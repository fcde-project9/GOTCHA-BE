package com.gotcha.domain.user.service;

import com.gotcha.domain.user.entity.PermissionType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.entity.UserPermission;
import com.gotcha.domain.user.entity.UserPermissionHistory;
import com.gotcha.domain.user.repository.UserPermissionHistoryRepository;
import com.gotcha.domain.user.repository.UserPermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 권한 관리 Service
 *
 * 주요 기능:
 * - 권한 동의 여부 확인
 * - 권한 동의 상태 업데이트 (+ 이력 자동 저장)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPermissionService {

    private final UserPermissionRepository userPermissionRepository;
    private final UserPermissionHistoryRepository userPermissionHistoryRepository;

    /**
     * 특정 권한 동의 여부 확인
     *
     * @param userId 사용자 ID
     * @param permissionType 권한 타입
     * @return true: 동의함, false: 미동의 또는 거부
     */
    public boolean isPermissionGranted(Long userId, PermissionType permissionType) {
        return userPermissionRepository.findByUserIdAndPermissionType(userId, permissionType)
                .map(UserPermission::getIsAgreed)
                .orElse(false);
    }

    /**
     * 권한 동의 상태 업데이트
     *
     * 동작:
     * 1. UserPermission 테이블 업데이트 (없으면 생성)
     * 2. UserPermissionHistory에 이력 저장
     *
     * @param user 사용자 엔티티
     * @param permissionType 권한 타입
     * @param isAgreed 동의 여부
     * @param deviceInfo 디바이스 정보 (nullable)
     */
    @Transactional
    public void updatePermission(User user, PermissionType permissionType, Boolean isAgreed,
                                 String deviceInfo) {
        Long userId = user.getId();

        // 1. 현재 상태 업데이트 (없으면 생성)
        UserPermission permission = userPermissionRepository
                .findByUserIdAndPermissionType(userId, permissionType)
                .orElseGet(() -> UserPermission.builder()
                        .user(user)
                        .permissionType(permissionType)
                        .isAgreed(false)
                        .build());

        permission.updateAgreement(isAgreed);
        userPermissionRepository.save(permission);

        // 2. 이력 저장
        UserPermissionHistory history = UserPermissionHistory.builder()
                .userId(userId)
                .permissionType(permissionType)
                .isAgreed(isAgreed)
                .deviceInfo(deviceInfo)
                .build();
        userPermissionHistoryRepository.save(history);

        log.info("Permission updated - userId: {}, type: {}, agreed: {}", userId, permissionType, isAgreed);
    }
}
