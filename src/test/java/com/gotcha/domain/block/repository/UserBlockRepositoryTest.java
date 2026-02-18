package com.gotcha.domain.block.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gotcha.config.TestcontainersConfig;
import com.gotcha.domain.block.entity.UserBlock;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
class UserBlockRepositoryTest {

    @Autowired
    private UserBlockRepository userBlockRepository;

    @Autowired
    private UserRepository userRepository;

    private User blocker;
    private User blocked1;
    private User blocked2;

    @BeforeEach
    void setUp() {
        blocker = userRepository.save(User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("blocker123")
                .nickname("차단자")
                .build());

        blocked1 = userRepository.save(User.builder()
                .socialType(SocialType.GOOGLE)
                .socialId("blocked1_123")
                .nickname("피차단자1")
                .build());

        blocked2 = userRepository.save(User.builder()
                .socialType(SocialType.NAVER)
                .socialId("blocked2_123")
                .nickname("피차단자2")
                .build());
    }

    @Nested
    @DisplayName("차단 존재 여부 확인")
    class ExistsByBlockerIdAndBlockedId {

        @Test
        @DisplayName("차단이 존재하면 true 반환")
        void existsBlock_Success() {
            // given
            userBlockRepository.save(UserBlock.builder()
                    .blocker(blocker)
                    .blocked(blocked1)
                    .build());

            // when
            boolean exists = userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blocked1.getId());

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("차단이 없으면 false 반환")
        void notExistsBlock() {
            // when
            boolean exists = userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blocked1.getId());

            // then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("역방향 차단은 별개임")
        void reverseBlock_NotDuplicate() {
            // given - blocker가 blocked1을 차단
            userBlockRepository.save(UserBlock.builder()
                    .blocker(blocker)
                    .blocked(blocked1)
                    .build());

            // when - blocked1이 blocker를 차단했는지 확인
            boolean exists = userBlockRepository.existsByBlockerIdAndBlockedId(blocked1.getId(), blocker.getId());

            // then - 역방향은 별개이므로 false
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("차단 조회")
    class FindByBlockerIdAndBlockedId {

        @Test
        @DisplayName("차단 조회 성공")
        void findBlock_Success() {
            // given
            UserBlock saved = userBlockRepository.save(UserBlock.builder()
                    .blocker(blocker)
                    .blocked(blocked1)
                    .build());

            // when
            Optional<UserBlock> found = userBlockRepository.findByBlockerIdAndBlockedId(
                    blocker.getId(), blocked1.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("차단이 없으면 빈 Optional 반환")
        void findBlock_NotFound() {
            // when
            Optional<UserBlock> found = userBlockRepository.findByBlockerIdAndBlockedId(
                    blocker.getId(), blocked1.getId());

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("차단한 사용자 ID 목록 조회")
    class FindBlockedUserIdsByBlockerId {

        @Test
        @DisplayName("차단한 사용자 ID 목록 조회 성공")
        void findBlockedUserIds_Success() {
            // given
            userBlockRepository.save(UserBlock.builder()
                    .blocker(blocker)
                    .blocked(blocked1)
                    .build());
            userBlockRepository.save(UserBlock.builder()
                    .blocker(blocker)
                    .blocked(blocked2)
                    .build());

            // when
            List<Long> blockedIds = userBlockRepository.findBlockedUserIdsByBlockerId(blocker.getId());

            // then
            assertThat(blockedIds).hasSize(2);
            assertThat(blockedIds).containsExactlyInAnyOrder(blocked1.getId(), blocked2.getId());
        }

        @Test
        @DisplayName("차단한 사용자가 없으면 빈 리스트 반환")
        void findBlockedUserIds_Empty() {
            // when
            List<Long> blockedIds = userBlockRepository.findBlockedUserIdsByBlockerId(blocker.getId());

            // then
            assertThat(blockedIds).isEmpty();
        }
    }

    @Nested
    @DisplayName("차단 목록 조회 (페이지네이션)")
    class FindAllByBlockerIdWithBlocked {

        @Test
        @DisplayName("차단 목록 조회 성공")
        void findBlocks_Success() {
            // given
            userBlockRepository.save(UserBlock.builder()
                    .blocker(blocker)
                    .blocked(blocked1)
                    .build());
            userBlockRepository.save(UserBlock.builder()
                    .blocker(blocker)
                    .blocked(blocked2)
                    .build());

            // when
            Page<UserBlock> page = userBlockRepository.findAllByBlockerIdWithBlocked(
                    blocker.getId(), PageRequest.of(0, 20));

            // then
            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("페이지네이션 동작 확인")
        void findBlocks_Pagination() {
            // given
            userBlockRepository.save(UserBlock.builder()
                    .blocker(blocker)
                    .blocked(blocked1)
                    .build());
            userBlockRepository.save(UserBlock.builder()
                    .blocker(blocker)
                    .blocked(blocked2)
                    .build());

            // when
            Page<UserBlock> page = userBlockRepository.findAllByBlockerIdWithBlocked(
                    blocker.getId(), PageRequest.of(0, 1));

            // then
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("차단 없으면 빈 페이지 반환")
        void findBlocks_Empty() {
            // when
            Page<UserBlock> page = userBlockRepository.findAllByBlockerIdWithBlocked(
                    blocker.getId(), PageRequest.of(0, 20));

            // then
            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("차단 삭제")
    class DeleteOperations {

        @Test
        @DisplayName("특정 차단 삭제")
        void deleteByBlockerIdAndBlockedId() {
            // given
            userBlockRepository.save(UserBlock.builder()
                    .blocker(blocker)
                    .blocked(blocked1)
                    .build());

            assertThat(userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blocked1.getId())).isTrue();

            // when
            userBlockRepository.deleteByBlockerIdAndBlockedId(blocker.getId(), blocked1.getId());
            userBlockRepository.flush();

            // then
            assertThat(userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blocked1.getId())).isFalse();
        }

        @Test
        @DisplayName("사용자 관련 모든 차단 삭제 (차단자로서)")
        void deleteAllByUserId_AsBlocker() {
            // given
            userBlockRepository.save(UserBlock.builder()
                    .blocker(blocker)
                    .blocked(blocked1)
                    .build());
            userBlockRepository.save(UserBlock.builder()
                    .blocker(blocker)
                    .blocked(blocked2)
                    .build());

            assertThat(userBlockRepository.findBlockedUserIdsByBlockerId(blocker.getId())).hasSize(2);

            // when
            userBlockRepository.deleteAllByUserId(blocker.getId());
            userBlockRepository.flush();

            // then
            assertThat(userBlockRepository.findBlockedUserIdsByBlockerId(blocker.getId())).isEmpty();
        }

        @Test
        @DisplayName("사용자 관련 모든 차단 삭제 (차단당한 사용자로서)")
        void deleteAllByUserId_AsBlocked() {
            // given - blocked1이 blocker에 의해 차단됨
            userBlockRepository.save(UserBlock.builder()
                    .blocker(blocker)
                    .blocked(blocked1)
                    .build());

            assertThat(userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blocked1.getId())).isTrue();

            // when - blocked1이 탈퇴하여 관련 차단 정보 삭제
            userBlockRepository.deleteAllByUserId(blocked1.getId());
            userBlockRepository.flush();

            // then - blocker의 차단 목록에서 blocked1이 제거됨
            assertThat(userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blocked1.getId())).isFalse();
        }

        @Test
        @DisplayName("사용자 관련 모든 차단 삭제 (양쪽 모두)")
        void deleteAllByUserId_BothSides() {
            // given - blocked1이 blocker를 차단, blocker가 blocked1을 차단
            userBlockRepository.save(UserBlock.builder()
                    .blocker(blocker)
                    .blocked(blocked1)
                    .build());
            userBlockRepository.save(UserBlock.builder()
                    .blocker(blocked1)
                    .blocked(blocker)
                    .build());

            assertThat(userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blocked1.getId())).isTrue();
            assertThat(userBlockRepository.existsByBlockerIdAndBlockedId(blocked1.getId(), blocker.getId())).isTrue();

            // when - blocked1이 탈퇴하여 관련 모든 차단 정보 삭제
            userBlockRepository.deleteAllByUserId(blocked1.getId());
            userBlockRepository.flush();

            // then - blocked1 관련 모든 차단 정보가 삭제됨
            assertThat(userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blocked1.getId())).isFalse();
            assertThat(userBlockRepository.existsByBlockerIdAndBlockedId(blocked1.getId(), blocker.getId())).isFalse();
        }
    }
}
