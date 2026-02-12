package com.gotcha.domain.user.service;

import com.gotcha.domain.auth.exception.AuthException;
import com.gotcha.domain.user.dto.AdminUserListResponse;
import com.gotcha.domain.user.dto.AdminUserResponse;
import com.gotcha.domain.user.dto.UpdateUserStatusRequest;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.entity.UserStatus;
import com.gotcha.domain.user.exception.UserException;
import com.gotcha.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private static final Set<Integer> ALLOWED_SUSPENSION_HOURS = Set.of(1, 12, 24, 72, 120, 168, 336, 720);

    private final UserRepository userRepository;

    /**
     * 사용자 목록 조회 (관리자 전용)
     * - status로 필터링 가능 (ACTIVE, SUSPENDED, BANNED)
     * - 탈퇴한 사용자 제외
     * - 페이징 지원
     */
    public AdminUserListResponse getUsers(UserStatus status, Pageable pageable) {
        Page<User> userPage = userRepository.findAllWithStatusFilter(status, pageable);
        Page<AdminUserResponse> responsePage = userPage.map(AdminUserResponse::from);
        return AdminUserListResponse.from(responsePage);
    }

    /**
     * 사용자 상세 조회 (관리자 전용)
     */
    public AdminUserResponse getUser(Long userId) {
        User user = findUserOrThrow(userId);
        return AdminUserResponse.from(user);
    }

    /**
     * 사용자 상태 변경 (관리자 전용)
     * - SUSPENDED: 기간 지정 정지 (허용 시간: 1, 12, 24, 72, 120, 168, 336, 720)
     * - BANNED: 영구 차단
     * - ACTIVE: 제재 해제
     * - ADMIN 계정 및 탈퇴한 사용자는 변경 불가
     */
    @Transactional
    public AdminUserResponse updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        User user = findUserOrThrow(userId);

        validateNotSelf(user);
        validateNotDeleted(user);

        switch (request.status()) {
            case SUSPENDED -> suspendUser(user, request.suspensionHours());
            case BANNED -> banUser(user);
            case ACTIVE -> activateUser(user);
            default -> throw AuthException.forbidden();
        }

        log.info("User status updated - userId: {}, newStatus: {}, suspendedUntil: {}",
                userId, user.getStatus(), user.getSuspendedUntil());

        return AdminUserResponse.from(user);
    }

    /**
     * 사용자 기간 정지 처리
     * - 허용된 정지 시간이 아니면 예외 발생
     * - 현재 시각 기준으로 정지 만료 시각 계산 후 적용
     */
    private void suspendUser(User user, Integer suspensionHours) {
        if (suspensionHours == null || !ALLOWED_SUSPENSION_HOURS.contains(suspensionHours)) {
            throw UserException.invalidSuspensionHours();
        }
        LocalDateTime until = LocalDateTime.now().plusHours(suspensionHours);
        user.suspend(until);
    }

    /**
     * 사용자 영구 차단 처리
     */
    private void banUser(User user) {
        user.ban();
    }

    /**
     * 사용자 제재 해제 (ACTIVE 상태로 복구)
     */
    private void activateUser(User user) {
        user.activate();
    }

    /**
     * 사용자 조회. 존재하지 않으면 UserException 발생
     */
    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> UserException.notFound(userId));
    }

    /**
     * 관리자 계정은 제재 대상에서 제외
     */
    private void validateNotSelf(User user) {
        if (user.isAdmin()) {
            throw AuthException.forbidden();
        }
    }

    /**
     * 탈퇴한 사용자는 상태 변경 불가
     */
    private void validateNotDeleted(User user) {
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw UserException.alreadyDeleted(user.getId());
        }
    }
}
