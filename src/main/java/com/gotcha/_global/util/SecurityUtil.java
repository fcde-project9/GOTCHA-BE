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
        // TODO: 사용자 삭제 시 별도 에러 코드(A010: 사용자를 찾을 수 없습니다) 분리 검토
        // 현재는 unauthorized로 처리하지만, 탈퇴/삭제된 사용자 구분이 필요하면 userNotFound 추가
        return userRepository.findById(userId)
                .orElseThrow(AuthException::unauthorized);
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
