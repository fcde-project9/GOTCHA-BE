package com.gotcha.domain.push.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.gotcha._global.config.PushProperties;
import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.push.dto.DeviceTokenRegisterRequest;
import com.gotcha.domain.push.entity.DevicePlatform;
import com.gotcha.domain.push.entity.DeviceToken;
import com.gotcha.domain.push.exception.PushException;
import com.gotcha.domain.push.repository.DeviceTokenRepository;
import com.gotcha.domain.push.repository.PushSubscriptionRepository;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @InjectMocks
    private PushNotificationService pushNotificationService;

    @Mock
    private PushSubscriptionRepository pushSubscriptionRepository;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private PushProperties pushProperties;

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private ObjectMapper objectMapper;

    private User createTestUser(Long id) {
        User user = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("social-" + id)
                .nickname("user-" + id)
                .build();
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return user;
    }

    @Test
    @DisplayName("기기 등록 - 신규 토큰 저장 성공")
    void registerDevice_newToken_savesDeviceToken() {
        // given
        User user = createTestUser(1L);
        given(securityUtil.getCurrentUser()).willReturn(user);
        given(deviceTokenRepository.findByDeviceToken("new-token")).willReturn(Optional.empty());

        DeviceTokenRegisterRequest request = new DeviceTokenRegisterRequest("new-token", DevicePlatform.IOS);

        // when
        pushNotificationService.registerDevice(request);

        // then
        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).save(captor.capture());

        DeviceToken saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getDeviceToken()).isEqualTo("new-token");
        assertThat(saved.getPlatform()).isEqualTo(DevicePlatform.IOS);
    }

    @Test
    @DisplayName("기기 등록 - 기존 토큰 소유자 이전")
    void registerDevice_existingToken_updatesOwner() {
        // given
        User newUser = createTestUser(2L);
        User oldUser = createTestUser(1L);
        given(securityUtil.getCurrentUser()).willReturn(newUser);

        DeviceToken existingToken = DeviceToken.builder()
                .user(oldUser)
                .deviceToken("existing-token")
                .platform(DevicePlatform.IOS)
                .build();
        given(deviceTokenRepository.findByDeviceToken("existing-token")).willReturn(Optional.of(existingToken));

        DeviceTokenRegisterRequest request = new DeviceTokenRegisterRequest("existing-token", DevicePlatform.IOS);

        // when
        pushNotificationService.registerDevice(request);

        // then
        assertThat(existingToken.getUser()).isEqualTo(newUser);
        verify(deviceTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("기기 해제 - 본인 토큰 삭제 성공")
    void unregisterDevice_ownToken_deletesSuccessfully() {
        // given
        Long userId = 1L;
        given(securityUtil.getCurrentUserId()).willReturn(userId);
        given(deviceTokenRepository.existsByUserIdAndDeviceToken(userId, "my-token")).willReturn(true);

        // when
        pushNotificationService.unregisterDevice("my-token");

        // then
        verify(deviceTokenRepository).deleteByUserIdAndDeviceToken(userId, "my-token");
    }

    @Test
    @DisplayName("기기 해제 - 존재하지 않는 토큰이면 예외 발생")
    void unregisterDevice_notFound_throwsException() {
        // given
        Long userId = 1L;
        given(securityUtil.getCurrentUserId()).willReturn(userId);
        given(deviceTokenRepository.existsByUserIdAndDeviceToken(userId, "unknown-token")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> pushNotificationService.unregisterDevice("unknown-token"))
                .isInstanceOf(PushException.class);
    }

    @Test
    @DisplayName("VAPID 공개키 조회 - 키가 설정된 경우")
    void getVapidPublicKey_configured_returnsKey() {
        // given
        PushProperties.Vapid vapid = new PushProperties.Vapid();
        vapid.setPublicKey("test-public-key");
        given(pushProperties.getVapid()).willReturn(vapid);

        // when
        var result = pushNotificationService.getVapidPublicKey();

        // then
        assertThat(result.publicKey()).isEqualTo("test-public-key");
    }

    @Test
    @DisplayName("VAPID 공개키 조회 - 키가 미설정이면 예외 발생")
    void getVapidPublicKey_notConfigured_throwsException() {
        // given
        PushProperties.Vapid vapid = new PushProperties.Vapid();
        vapid.setPublicKey("");
        given(pushProperties.getVapid()).willReturn(vapid);

        // when & then
        assertThatThrownBy(() -> pushNotificationService.getVapidPublicKey())
                .isInstanceOf(PushException.class);
    }
}
