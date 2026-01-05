package com.gotcha.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gotcha.config.TestcontainersConfig;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("소셜 타입과 소셜 ID로 사용자 조회")
    void findBySocialTypeAndSocialId() {
        // given
        User user = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("kakao123")
                .nickname("테스트유저")
                .build();
        userRepository.save(user);

        // when
        Optional<User> found = userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, "kakao123");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getNickname()).isEqualTo("테스트유저");
    }

    @Test
    @DisplayName("존재하지 않는 소셜 ID로 조회 시 빈 Optional 반환")
    void findBySocialTypeAndSocialId_NotFound() {
        // when
        Optional<User> found = userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, "nonexistent");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("닉네임으로 사용자 조회")
    void findByNickname() {
        // given
        User user = User.builder()
                .socialType(SocialType.GOOGLE)
                .socialId("google123")
                .nickname("유니크닉네임")
                .build();
        userRepository.save(user);

        // when
        Optional<User> found = userRepository.findByNickname("유니크닉네임");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getSocialType()).isEqualTo(SocialType.GOOGLE);
    }

    @Test
    @DisplayName("닉네임 존재 여부 확인 - 존재하는 경우")
    void existsByNickname_True() {
        // given
        User user = User.builder()
                .socialType(SocialType.NAVER)
                .socialId("naver123")
                .nickname("존재하는닉네임")
                .build();
        userRepository.save(user);

        // when
        boolean exists = userRepository.existsByNickname("존재하는닉네임");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("닉네임 존재 여부 확인 - 존재하지 않는 경우")
    void existsByNickname_False() {
        // when
        boolean exists = userRepository.existsByNickname("없는닉네임");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("동일 소셜ID, 다른 소셜타입으로 조회 시 빈 Optional 반환")
    void findBySocialTypeAndSocialId_DifferentSocialType() {
        // given
        User user = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("same123")
                .nickname("카카오유저")
                .build();
        userRepository.save(user);

        // when - 같은 socialId지만 다른 socialType으로 조회
        Optional<User> found = userRepository.findBySocialTypeAndSocialId(SocialType.GOOGLE, "same123");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 닉네임으로 조회 시 빈 Optional 반환")
    void findByNickname_NotFound() {
        // when
        Optional<User> found = userRepository.findByNickname("존재하지않는닉네임");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("빈 문자열 닉네임으로 조회")
    void findByNickname_EmptyString() {
        // when
        Optional<User> found = userRepository.findByNickname("");

        // then
        assertThat(found).isEmpty();
    }
}
