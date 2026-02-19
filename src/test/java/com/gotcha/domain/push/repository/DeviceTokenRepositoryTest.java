package com.gotcha.domain.push.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gotcha.config.TestcontainersConfig;
import com.gotcha.domain.push.entity.DevicePlatform;
import com.gotcha.domain.push.entity.DeviceToken;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
class DeviceTokenRepositoryTest {

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("user123")
                .nickname("테스트유저")
                .build());

        otherUser = userRepository.save(User.builder()
                .socialType(SocialType.GOOGLE)
                .socialId("other123")
                .nickname("다른유저")
                .build());
    }

    @Test
    @DisplayName("사용자의 디바이스 토큰 목록 조회 - 토큰이 있는 경우")
    void findAllByUserId_hasTokens_returnsList() {
        // given
        deviceTokenRepository.save(DeviceToken.builder()
                .user(user).deviceToken("token-1").platform(DevicePlatform.IOS).build());
        deviceTokenRepository.save(DeviceToken.builder()
                .user(user).deviceToken("token-2").platform(DevicePlatform.IOS).build());

        // when
        List<DeviceToken> result = deviceTokenRepository.findAllByUserId(user.getId());

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("사용자의 디바이스 토큰 목록 조회 - 토큰이 없는 경우")
    void findAllByUserId_noTokens_returnsEmpty() {
        // when
        List<DeviceToken> result = deviceTokenRepository.findAllByUserId(user.getId());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("디바이스 토큰으로 조회 - 존재하는 경우")
    void findByDeviceToken_exists_returnsToken() {
        // given
        deviceTokenRepository.save(DeviceToken.builder()
                .user(user).deviceToken("apns-token-123").platform(DevicePlatform.IOS).build());

        // when
        Optional<DeviceToken> found = deviceTokenRepository.findByDeviceToken("apns-token-123");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("디바이스 토큰으로 조회 - 존재하지 않는 경우")
    void findByDeviceToken_notExists_returnsEmpty() {
        // when
        Optional<DeviceToken> found = deviceTokenRepository.findByDeviceToken("non-existent");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("소유권 확인 - 본인 토큰이면 true")
    void existsByUserIdAndDeviceToken_ownToken_returnsTrue() {
        // given
        deviceTokenRepository.save(DeviceToken.builder()
                .user(user).deviceToken("my-token").platform(DevicePlatform.IOS).build());

        // when
        boolean exists = deviceTokenRepository.existsByUserIdAndDeviceToken(user.getId(), "my-token");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("소유권 확인 - 다른 사용자 토큰이면 false")
    void existsByUserIdAndDeviceToken_otherUser_returnsFalse() {
        // given
        deviceTokenRepository.save(DeviceToken.builder()
                .user(otherUser).deviceToken("other-token").platform(DevicePlatform.IOS).build());

        // when
        boolean exists = deviceTokenRepository.existsByUserIdAndDeviceToken(user.getId(), "other-token");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("사용자+토큰으로 삭제 - 본인 토큰만 삭제됨")
    void deleteByUserIdAndDeviceToken_deletesOnlyOwn() {
        // given
        deviceTokenRepository.save(DeviceToken.builder()
                .user(user).deviceToken("user-token").platform(DevicePlatform.IOS).build());
        deviceTokenRepository.save(DeviceToken.builder()
                .user(otherUser).deviceToken("other-token").platform(DevicePlatform.IOS).build());

        // when
        deviceTokenRepository.deleteByUserIdAndDeviceToken(user.getId(), "user-token");
        deviceTokenRepository.flush();

        // then
        assertThat(deviceTokenRepository.findByDeviceToken("user-token")).isEmpty();
        assertThat(deviceTokenRepository.findByDeviceToken("other-token")).isPresent();
    }

    @Test
    @DisplayName("사용자의 모든 토큰 삭제 - 다른 사용자 영향 없음")
    void deleteAllByUserId_deletesAll() {
        // given
        deviceTokenRepository.save(DeviceToken.builder()
                .user(user).deviceToken("token-a").platform(DevicePlatform.IOS).build());
        deviceTokenRepository.save(DeviceToken.builder()
                .user(user).deviceToken("token-b").platform(DevicePlatform.ANDROID).build());
        deviceTokenRepository.save(DeviceToken.builder()
                .user(otherUser).deviceToken("other-token").platform(DevicePlatform.IOS).build());

        // when
        deviceTokenRepository.deleteAllByUserId(user.getId());
        deviceTokenRepository.flush();

        // then
        assertThat(deviceTokenRepository.findAllByUserId(user.getId())).isEmpty();
        assertThat(deviceTokenRepository.findAllByUserId(otherUser.getId())).hasSize(1);
    }

    @Test
    @DisplayName("플랫폼별 필터링 - IOS만 조회")
    void findAllByUserIdAndPlatform_iosOnly() {
        // given
        deviceTokenRepository.save(DeviceToken.builder()
                .user(user).deviceToken("ios-token").platform(DevicePlatform.IOS).build());
        deviceTokenRepository.save(DeviceToken.builder()
                .user(user).deviceToken("android-token").platform(DevicePlatform.ANDROID).build());

        // when
        List<DeviceToken> iosTokens = deviceTokenRepository
                .findAllByUserIdAndPlatform(user.getId(), DevicePlatform.IOS);

        // then
        assertThat(iosTokens).hasSize(1);
        assertThat(iosTokens.get(0).getPlatform()).isEqualTo(DevicePlatform.IOS);
    }

    @Test
    @DisplayName("소유자 변경 후 조회 - 새 소유자로 조회 가능")
    void updateUser_ownershipTransfer() {
        // given
        DeviceToken token = deviceTokenRepository.save(DeviceToken.builder()
                .user(user).deviceToken("shared-token").platform(DevicePlatform.IOS).build());

        // when
        token.updateUser(otherUser);
        deviceTokenRepository.flush();

        // then
        assertThat(deviceTokenRepository.existsByUserIdAndDeviceToken(otherUser.getId(), "shared-token"))
                .isTrue();
        assertThat(deviceTokenRepository.existsByUserIdAndDeviceToken(user.getId(), "shared-token"))
                .isFalse();
    }
}
