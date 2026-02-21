package com.gotcha.domain.block.service;

import com.gotcha._global.common.PageResponse;
import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.block.dto.BlockResponse;
import com.gotcha.domain.block.dto.BlockedUserResponse;
import com.gotcha.domain.block.entity.UserBlock;
import com.gotcha.domain.block.exception.BlockException;
import com.gotcha.domain.block.repository.UserBlockRepository;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.exception.UserException;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserBlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;

    @Transactional
    public BlockResponse blockUser(Long blockedUserId) {
        User currentUser = securityUtil.getCurrentUser();
        Long blockerId = currentUser.getId();

        if (blockerId.equals(blockedUserId)) {
            throw BlockException.cannotBlockSelf();
        }

        User blockedUser = userRepository.findById(blockedUserId)
                .orElseThrow(() -> UserException.notFound(blockedUserId));

        // 탈퇴했거나 비활성 상태의 사용자는 차단 불가
        if (blockedUser.getIsDeleted() || !blockedUser.isActive()) {
            throw BlockException.invalidBlockTarget(blockedUserId);
        }

        if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedUserId)) {
            throw BlockException.alreadyBlocked();
        }

        UserBlock userBlock = UserBlock.builder()
                .blocker(currentUser)
                .blocked(blockedUser)
                .build();

        try {
            userBlockRepository.save(userBlock);
        } catch (DataIntegrityViolationException e) {
            // TOCTOU 경합 조건: 동시 요청으로 인한 유니크 제약조건 위반
            throw BlockException.alreadyBlocked();
        }

        log.info("User blocked - blockerId: {}, blockedUserId: {}", blockerId, blockedUserId);

        return BlockResponse.from(userBlock);
    }

    @Transactional
    public void unblockUser(Long blockedUserId) {
        Long blockerId = securityUtil.getCurrentUserId();

        UserBlock userBlock = userBlockRepository.findByBlockerIdAndBlockedId(blockerId, blockedUserId)
                .orElseThrow(() -> BlockException.notFound(blockedUserId));

        userBlockRepository.delete(userBlock);

        log.info("User unblocked - blockerId: {}, blockedUserId: {}", blockerId, blockedUserId);
    }

    public PageResponse<BlockedUserResponse> getMyBlocks(Pageable pageable) {
        Long blockerId = securityUtil.getCurrentUserId();

        Page<UserBlock> blockPage = userBlockRepository.findAllByBlockerIdWithBlocked(blockerId, pageable);

        List<BlockedUserResponse> responses = blockPage.getContent().stream()
                .map(BlockedUserResponse::from)
                .toList();

        return PageResponse.from(blockPage, responses);
    }

    public List<Long> getBlockedUserIds(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return userBlockRepository.findBlockedUserIdsByBlockerId(userId);
    }
}
