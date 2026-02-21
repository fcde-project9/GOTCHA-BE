package com.gotcha.domain.block.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gotcha._global.common.PageResponse;
import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.block.dto.BlockResponse;
import com.gotcha.domain.block.dto.BlockedUserResponse;
import com.gotcha.domain.block.entity.UserBlock;
import com.gotcha.domain.block.exception.BlockException;
import com.gotcha.domain.block.repository.UserBlockRepository;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.exception.UserException;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserBlockServiceTest {

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private UserBlockService userBlockService;

    private User blocker;
    private User blockedUser;

    @BeforeEach
    void setUp() {
        blocker = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("blocker123")
                .nickname("차단자")
                .build();
        ReflectionTestUtils.setField(blocker, "id", 1L);

        blockedUser = User.builder()
                .socialType(SocialType.GOOGLE)
                .socialId("blocked123")
                .nickname("피차단자")
                .build();
        ReflectionTestUtils.setField(blockedUser, "id", 2L);
    }

    @Nested
    @DisplayName("사용자 차단")
    class BlockUser {

        @Test
        @DisplayName("차단 성공")
        void blockUser_Success() {
            // given
            when(securityUtil.getCurrentUser()).thenReturn(blocker);
            when(userRepository.findById(blockedUser.getId())).thenReturn(Optional.of(blockedUser));
            when(userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blockedUser.getId()))
                    .thenReturn(false);
            when(userBlockRepository.save(any(UserBlock.class))).thenAnswer(invocation -> {
                UserBlock saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 1L);
                return saved;
            });

            // when
            BlockResponse response = userBlockService.blockUser(blockedUser.getId());

            // then
            assertThat(response.blockedUserId()).isEqualTo(blockedUser.getId());

            ArgumentCaptor<UserBlock> captor = ArgumentCaptor.forClass(UserBlock.class);
            verify(userBlockRepository).save(captor.capture());
            UserBlock savedBlock = captor.getValue();
            assertThat(savedBlock.getBlocker().getId()).isEqualTo(blocker.getId());
            assertThat(savedBlock.getBlocked().getId()).isEqualTo(blockedUser.getId());
        }

        @Test
        @DisplayName("본인을 차단할 수 없음")
        void blockUser_CannotBlockSelf_Fail() {
            // given
            when(securityUtil.getCurrentUser()).thenReturn(blocker);

            // when & then
            assertThatThrownBy(() -> userBlockService.blockUser(blocker.getId()))
                    .isInstanceOf(BlockException.class)
                    .hasMessageContaining("본인을 차단할 수 없습니다");
        }

        @Test
        @DisplayName("존재하지 않는 사용자 차단 불가")
        void blockUser_UserNotFound_Fail() {
            // given
            when(securityUtil.getCurrentUser()).thenReturn(blocker);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userBlockService.blockUser(999L))
                    .isInstanceOf(UserException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("이미 차단한 사용자 중복 차단 불가")
        void blockUser_AlreadyBlocked_Fail() {
            // given
            when(securityUtil.getCurrentUser()).thenReturn(blocker);
            when(userRepository.findById(blockedUser.getId())).thenReturn(Optional.of(blockedUser));
            when(userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blockedUser.getId()))
                    .thenReturn(true);

            // when & then
            assertThatThrownBy(() -> userBlockService.blockUser(blockedUser.getId()))
                    .isInstanceOf(BlockException.class)
                    .hasMessageContaining("이미 차단한 사용자입니다");
        }

        @Test
        @DisplayName("탈퇴한 사용자 차단 불가")
        void blockUser_DeletedUser_Fail() {
            // given
            User deletedUser = User.builder()
                    .socialType(SocialType.KAKAO)
                    .socialId("deleted123")
                    .nickname("탈퇴유저")
                    .build();
            ReflectionTestUtils.setField(deletedUser, "id", 3L);
            ReflectionTestUtils.setField(deletedUser, "isDeleted", true);

            when(securityUtil.getCurrentUser()).thenReturn(blocker);
            when(userRepository.findById(3L)).thenReturn(Optional.of(deletedUser));

            // when & then
            assertThatThrownBy(() -> userBlockService.blockUser(3L))
                    .isInstanceOf(BlockException.class)
                    .hasMessageContaining("차단할 수 없는 사용자입니다");
        }

        @Test
        @DisplayName("정지된 사용자 차단 불가")
        void blockUser_SuspendedUser_Fail() {
            // given
            User suspendedUser = User.builder()
                    .socialType(SocialType.KAKAO)
                    .socialId("suspended123")
                    .nickname("정지유저")
                    .build();
            ReflectionTestUtils.setField(suspendedUser, "id", 4L);
            ReflectionTestUtils.setField(suspendedUser, "status", com.gotcha.domain.user.entity.UserStatus.SUSPENDED);

            when(securityUtil.getCurrentUser()).thenReturn(blocker);
            when(userRepository.findById(4L)).thenReturn(Optional.of(suspendedUser));

            // when & then
            assertThatThrownBy(() -> userBlockService.blockUser(4L))
                    .isInstanceOf(BlockException.class)
                    .hasMessageContaining("차단할 수 없는 사용자입니다");
        }
    }

    @Nested
    @DisplayName("차단 해제")
    class UnblockUser {

        @Test
        @DisplayName("차단 해제 성공")
        void unblockUser_Success() {
            // given
            UserBlock userBlock = UserBlock.builder()
                    .blocker(blocker)
                    .blocked(blockedUser)
                    .build();
            ReflectionTestUtils.setField(userBlock, "id", 1L);

            when(securityUtil.getCurrentUserId()).thenReturn(blocker.getId());
            when(userBlockRepository.findByBlockerIdAndBlockedId(blocker.getId(), blockedUser.getId()))
                    .thenReturn(Optional.of(userBlock));

            // when
            userBlockService.unblockUser(blockedUser.getId());

            // then
            verify(userBlockRepository).delete(userBlock);
        }

        @Test
        @DisplayName("차단 정보가 없으면 예외 발생")
        void unblockUser_NotFound_Fail() {
            // given
            when(securityUtil.getCurrentUserId()).thenReturn(blocker.getId());
            when(userBlockRepository.findByBlockerIdAndBlockedId(blocker.getId(), blockedUser.getId()))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userBlockService.unblockUser(blockedUser.getId()))
                    .isInstanceOf(BlockException.class)
                    .hasMessageContaining("차단 정보를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("차단 목록 조회")
    class GetMyBlocks {

        @Test
        @DisplayName("차단 목록 조회 성공")
        void getMyBlocks_Success() {
            // given
            UserBlock userBlock = UserBlock.builder()
                    .blocker(blocker)
                    .blocked(blockedUser)
                    .build();
            ReflectionTestUtils.setField(userBlock, "id", 1L);

            Pageable pageable = PageRequest.of(0, 20);
            Page<UserBlock> blockPage = new PageImpl<>(List.of(userBlock), pageable, 1);

            when(securityUtil.getCurrentUserId()).thenReturn(blocker.getId());
            when(userBlockRepository.findAllByBlockerIdWithBlocked(blocker.getId(), pageable))
                    .thenReturn(blockPage);

            // when
            PageResponse<BlockedUserResponse> response = userBlockService.getMyBlocks(pageable);

            // then
            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).id()).isEqualTo(blockedUser.getId());
            assertThat(response.content().get(0).nickname()).isEqualTo(blockedUser.getNickname());
        }

        @Test
        @DisplayName("차단 목록이 없으면 빈 리스트 반환")
        void getMyBlocks_Empty() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Page<UserBlock> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(securityUtil.getCurrentUserId()).thenReturn(blocker.getId());
            when(userBlockRepository.findAllByBlockerIdWithBlocked(blocker.getId(), pageable))
                    .thenReturn(emptyPage);

            // when
            PageResponse<BlockedUserResponse> response = userBlockService.getMyBlocks(pageable);

            // then
            assertThat(response.content()).isEmpty();
        }
    }

    @Nested
    @DisplayName("차단한 사용자 ID 목록 조회")
    class GetBlockedUserIds {

        @Test
        @DisplayName("차단한 사용자 ID 목록 조회 성공")
        void getBlockedUserIds_Success() {
            // given
            List<Long> blockedIds = List.of(2L, 3L, 4L);
            when(userBlockRepository.findBlockedUserIdsByBlockerId(blocker.getId())).thenReturn(blockedIds);

            // when
            List<Long> result = userBlockService.getBlockedUserIds(blocker.getId());

            // then
            assertThat(result).containsExactly(2L, 3L, 4L);
        }

        @Test
        @DisplayName("userId가 null이면 빈 리스트 반환")
        void getBlockedUserIds_NullUserId_ReturnsEmptyList() {
            // when
            List<Long> result = userBlockService.getBlockedUserIds(null);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("차단한 사용자가 없으면 빈 리스트 반환")
        void getBlockedUserIds_NoBlocks_ReturnsEmptyList() {
            // given
            when(userBlockRepository.findBlockedUserIdsByBlockerId(blocker.getId())).thenReturn(List.of());

            // when
            List<Long> result = userBlockService.getBlockedUserIds(blocker.getId());

            // then
            assertThat(result).isEmpty();
        }
    }
}
