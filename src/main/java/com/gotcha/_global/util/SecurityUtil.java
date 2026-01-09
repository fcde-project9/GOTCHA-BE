package com.gotcha._global.util;

import com.gotcha.domain.auth.exception.AuthException;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(AuthException::unauthorized);

        // 탈퇴한 사용자 차단
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw AuthException.userDeleted();
        }

        return user;
    }

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // TODO: isAuthenticated() 체크 추가 검토 (방어적 코딩)
        // 현재는 principal 타입 체크로 충분하지만, 필요시 !authentication.isAuthenticated() 조건 추가
        if (authentication == null || authentication.getPrincipal() == null) {
            throw AuthException.unauthorized();
        }

        if (!(authentication.getPrincipal() instanceof Long)) {
            throw AuthException.unauthorized();
        }

        return (Long) authentication.getPrincipal();
    }
}
