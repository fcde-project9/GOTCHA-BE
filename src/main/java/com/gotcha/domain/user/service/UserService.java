package com.gotcha.domain.user.service;

import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.user.dto.UserNicknameResponse;
import com.gotcha.domain.user.dto.UserResponse;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.exception.UserException;
import com.gotcha.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final SecurityUtil securityUtil;
    private final UserRepository userRepository;

    public UserResponse getMyInfo() {
        User user = securityUtil.getCurrentUser();
        return UserResponse.from(user);
    }

    /**
     * 현재 로그인한 사용자의 닉네임 조회
     * @return 사용자 닉네임
     */
    public UserNicknameResponse getNickname() {
        User user = securityUtil.getCurrentUser();
        return UserNicknameResponse.from(user);
    }

    /**
     * 닉네임 변경
     * @param nickname 새 닉네임
     * @return 변경된 사용자 정보
     * @throws UserException 닉네임이 중복된 경우 (U001)
     */
    @Transactional
    public UserResponse updateNickname(String nickname) {
        log.info("updateNickname - nickname: {}", nickname);

        // 현재 로그인한 사용자 조회
        User currentUser = securityUtil.getCurrentUser();
        log.info("Current user ID: {}, current nickname: {}", currentUser.getId(), currentUser.getNickname());

        // 현재 닉네임과 동일한 경우 그대로 반환 (중복 체크 불필요)
        if (currentUser.getNickname().equals(nickname)) {
            log.info("Same nickname as current, skipping duplicate check");
            return UserResponse.from(currentUser);
        }

        // 닉네임 중복 체크
        if (userRepository.existsByNickname(nickname)) {
            log.warn("Duplicate nickname: {}", nickname);
            throw UserException.duplicateNickname(nickname);
        }

        // 닉네임 변경
        currentUser.updateNickname(nickname);
        log.info("Nickname updated successfully: {} -> {}", currentUser.getId(), nickname);

        return UserResponse.from(currentUser);
    }
}
