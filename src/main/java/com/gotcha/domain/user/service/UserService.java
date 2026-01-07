package com.gotcha.domain.user.service;

import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.user.dto.UserResponse;
import com.gotcha.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final SecurityUtil securityUtil;

    public UserResponse getMyInfo() {
        User user = securityUtil.getCurrentUser();
        return UserResponse.from(user);
    }
}
