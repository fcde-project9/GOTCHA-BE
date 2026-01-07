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
        return userRepository.findById(userId)
                .orElseThrow(AuthException::unauthorized);
    }

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw AuthException.unauthorized();
        }

        if (!(authentication.getPrincipal() instanceof Long)) {
            throw AuthException.unauthorized();
        }

        return (Long) authentication.getPrincipal();
    }
}
