package com.gotcha.domain.push.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gotcha.config.TestcontainersConfig;
import com.gotcha.domain.push.entity.PushSubscription;
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
class PushSubscriptionRepositoryTest {

    @Autowired
    private PushSubscriptionRepository pushSubscriptionRepository;

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
    @DisplayName("endpoint로 구독 조회 - 존재하는 경우")
    void findByEndpoint_exists_returnsSubscription() {
        // given
        String endpoint = "https://fcm.googleapis.com/fcm/send/test-endpoint";
        PushSubscription subscription = pushSubscriptionRepository.save(PushSubscription.builder()
                .user(user)
                .endpoint(endpoint)
                .p256dh("test-p256dh-key")
                .auth("test-auth-key")
                .build());

        // when
        Optional<PushSubscription> found = pushSubscriptionRepository.findByEndpoint(endpoint);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getEndpoint()).isEqualTo(endpoint);
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("endpoint로 구독 조회 - 존재하지 않는 경우 빈 Optional 반환")
    void findByEndpoint_notExists_returnsEmpty() {
        // when
        Optional<PushSubscription> found = pushSubscriptionRepository
                .findByEndpoint("https://non-existent-endpoint");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("endpoint 존재 여부 확인 - 존재하는 경우 true 반환")
    void existsByEndpoint_exists_returnsTrue() {
        // given
        String endpoint = "https://fcm.googleapis.com/fcm/send/test-endpoint";
        pushSubscriptionRepository.save(PushSubscription.builder()
                .user(user)
                .endpoint(endpoint)
                .p256dh("test-p256dh-key")
                .auth("test-auth-key")
                .build());

        // when
        boolean exists = pushSubscriptionRepository.existsByEndpoint(endpoint);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("endpoint 존재 여부 확인 - 존재하지 않는 경우 false 반환")
    void existsByEndpoint_notExists_returnsFalse() {
        // when
        boolean exists = pushSubscriptionRepository.existsByEndpoint("https://non-existent-endpoint");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("사용자 ID로 모든 구독 조회 - 구독 있는 경우")
    void findAllByUserId_hasSubscriptions_returnsList() {
        // given
        pushSubscriptionRepository.save(PushSubscription.builder()
                .user(user)
                .endpoint("https://endpoint1")
                .p256dh("key1")
                .auth("auth1")
                .build());
        pushSubscriptionRepository.save(PushSubscription.builder()
                .user(user)
                .endpoint("https://endpoint2")
                .p256dh("key2")
                .auth("auth2")
                .build());

        // when
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findAllByUserId(user.getId());

        // then
        assertThat(subscriptions).hasSize(2);
    }

    @Test
    @DisplayName("사용자 ID로 모든 구독 조회 - 구독 없는 경우 빈 리스트 반환")
    void findAllByUserId_noSubscriptions_returnsEmptyList() {
        // when
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findAllByUserId(user.getId());

        // then
        assertThat(subscriptions).isEmpty();
    }

    @Test
    @DisplayName("사용자 ID로 모든 구독 조회 - 존재하지 않는 사용자 ID")
    void findAllByUserId_nonExistentUser_returnsEmptyList() {
        // when
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findAllByUserId(999999L);

        // then
        assertThat(subscriptions).isEmpty();
    }

    @Test
    @DisplayName("endpoint로 구독 삭제 - 존재하는 경우")
    void deleteByEndpoint_exists_deletesSubscription() {
        // given
        String endpoint = "https://fcm.googleapis.com/fcm/send/test-endpoint";
        pushSubscriptionRepository.save(PushSubscription.builder()
                .user(user)
                .endpoint(endpoint)
                .p256dh("test-p256dh-key")
                .auth("test-auth-key")
                .build());

        // when
        pushSubscriptionRepository.deleteByEndpoint(endpoint);
        pushSubscriptionRepository.flush();

        // then
        assertThat(pushSubscriptionRepository.findByEndpoint(endpoint)).isEmpty();
    }

    @Test
    @DisplayName("endpoint로 구독 삭제 - 존재하지 않는 경우 에러 없이 진행")
    void deleteByEndpoint_notExists_noError() {
        // when & then - 존재하지 않는 데이터 삭제 시 에러 없음
        pushSubscriptionRepository.deleteByEndpoint("https://non-existent-endpoint");
        pushSubscriptionRepository.flush();
    }

    @Test
    @DisplayName("사용자 ID로 모든 구독 삭제 - 구독 있는 경우")
    void deleteAllByUserId_hasSubscriptions_deletesAll() {
        // given
        pushSubscriptionRepository.save(PushSubscription.builder()
                .user(user)
                .endpoint("https://endpoint1")
                .p256dh("key1")
                .auth("auth1")
                .build());
        pushSubscriptionRepository.save(PushSubscription.builder()
                .user(user)
                .endpoint("https://endpoint2")
                .p256dh("key2")
                .auth("auth2")
                .build());
        assertThat(pushSubscriptionRepository.findAllByUserId(user.getId())).hasSize(2);

        // when
        pushSubscriptionRepository.deleteAllByUserId(user.getId());
        pushSubscriptionRepository.flush();

        // then
        assertThat(pushSubscriptionRepository.findAllByUserId(user.getId())).isEmpty();
    }

    @Test
    @DisplayName("사용자 ID로 모든 구독 삭제 - 존재하지 않는 사용자 ID 에러 없이 진행")
    void deleteAllByUserId_nonExistentUser_noError() {
        // when & then - 존재하지 않는 사용자 삭제 시 에러 없음
        pushSubscriptionRepository.deleteAllByUserId(999999L);
        pushSubscriptionRepository.flush();
    }

    @Test
    @DisplayName("사용자 ID로 구독 삭제 시 다른 사용자 구독은 영향 없음")
    void deleteAllByUserId_doesNotAffectOtherUsers() {
        // given
        pushSubscriptionRepository.save(PushSubscription.builder()
                .user(user)
                .endpoint("https://endpoint1")
                .p256dh("key1")
                .auth("auth1")
                .build());
        pushSubscriptionRepository.save(PushSubscription.builder()
                .user(otherUser)
                .endpoint("https://endpoint2")
                .p256dh("key2")
                .auth("auth2")
                .build());

        // when
        pushSubscriptionRepository.deleteAllByUserId(user.getId());
        pushSubscriptionRepository.flush();

        // then
        assertThat(pushSubscriptionRepository.findAllByUserId(user.getId())).isEmpty();
        assertThat(pushSubscriptionRepository.findAllByUserId(otherUser.getId())).hasSize(1);
    }

    @Test
    @DisplayName("사용자 ID와 endpoint로 구독 조회 - 존재하는 경우")
    void findByUserIdAndEndpoint_exists_returnsSubscription() {
        // given
        String endpoint = "https://fcm.googleapis.com/fcm/send/test-endpoint";
        pushSubscriptionRepository.save(PushSubscription.builder()
                .user(user)
                .endpoint(endpoint)
                .p256dh("test-p256dh-key")
                .auth("test-auth-key")
                .build());

        // when
        Optional<PushSubscription> found = pushSubscriptionRepository
                .findByUserIdAndEndpoint(user.getId(), endpoint);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getEndpoint()).isEqualTo(endpoint);
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("사용자 ID와 endpoint로 구독 조회 - 다른 사용자의 endpoint는 조회 안됨")
    void findByUserIdAndEndpoint_otherUserEndpoint_returnsEmpty() {
        // given
        String endpoint = "https://fcm.googleapis.com/fcm/send/test-endpoint";
        pushSubscriptionRepository.save(PushSubscription.builder()
                .user(otherUser)
                .endpoint(endpoint)
                .p256dh("test-p256dh-key")
                .auth("test-auth-key")
                .build());

        // when
        Optional<PushSubscription> found = pushSubscriptionRepository
                .findByUserIdAndEndpoint(user.getId(), endpoint);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("사용자 ID와 endpoint로 구독 삭제 - 본인 구독만 삭제")
    void deleteByUserIdAndEndpoint_deletesOnlyOwnSubscription() {
        // given
        String endpoint = "https://fcm.googleapis.com/fcm/send/test-endpoint";
        pushSubscriptionRepository.save(PushSubscription.builder()
                .user(user)
                .endpoint(endpoint)
                .p256dh("test-p256dh-key")
                .auth("test-auth-key")
                .build());

        // when
        pushSubscriptionRepository.deleteByUserIdAndEndpoint(user.getId(), endpoint);
        pushSubscriptionRepository.flush();

        // then
        assertThat(pushSubscriptionRepository.findByUserIdAndEndpoint(user.getId(), endpoint)).isEmpty();
    }

    @Test
    @DisplayName("사용자 ID와 endpoint로 구독 삭제 - 다른 사용자의 endpoint는 삭제 안됨")
    void deleteByUserIdAndEndpoint_doesNotDeleteOtherUserSubscription() {
        // given
        String endpoint = "https://fcm.googleapis.com/fcm/send/test-endpoint";
        pushSubscriptionRepository.save(PushSubscription.builder()
                .user(otherUser)
                .endpoint(endpoint)
                .p256dh("test-p256dh-key")
                .auth("test-auth-key")
                .build());

        // when
        pushSubscriptionRepository.deleteByUserIdAndEndpoint(user.getId(), endpoint);
        pushSubscriptionRepository.flush();

        // then
        assertThat(pushSubscriptionRepository.findByEndpoint(endpoint)).isPresent();
    }

    @Test
    @DisplayName("사용자 ID와 endpoint 존재 여부 확인 - 본인 구독 존재하는 경우 true")
    void existsByUserIdAndEndpoint_ownSubscription_returnsTrue() {
        // given
        String endpoint = "https://fcm.googleapis.com/fcm/send/test-endpoint";
        pushSubscriptionRepository.save(PushSubscription.builder()
                .user(user)
                .endpoint(endpoint)
                .p256dh("test-p256dh-key")
                .auth("test-auth-key")
                .build());

        // when
        boolean exists = pushSubscriptionRepository.existsByUserIdAndEndpoint(user.getId(), endpoint);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("사용자 ID와 endpoint 존재 여부 확인 - 다른 사용자의 endpoint는 false")
    void existsByUserIdAndEndpoint_otherUserEndpoint_returnsFalse() {
        // given
        String endpoint = "https://fcm.googleapis.com/fcm/send/test-endpoint";
        pushSubscriptionRepository.save(PushSubscription.builder()
                .user(otherUser)
                .endpoint(endpoint)
                .p256dh("test-p256dh-key")
                .auth("test-auth-key")
                .build());

        // when
        boolean exists = pushSubscriptionRepository.existsByUserIdAndEndpoint(user.getId(), endpoint);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("사용자 ID와 endpoint 존재 여부 확인 - 존재하지 않는 경우 false")
    void existsByUserIdAndEndpoint_notExists_returnsFalse() {
        // when
        boolean exists = pushSubscriptionRepository
                .existsByUserIdAndEndpoint(user.getId(), "https://non-existent-endpoint");

        // then
        assertThat(exists).isFalse();
    }
}
