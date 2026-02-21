package com.gotcha.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.gotcha.domain.auth.oauth2.userinfo.OAuth2UserInfo;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuth2UserRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OAuth2UserRegistrationService registrationService;

    private static final String DEFAULT_PROFILE_IMAGE_URL = "https://example.com/default.png";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(registrationService, "defaultProfileImageUrl", DEFAULT_PROFILE_IMAGE_URL);
    }

    @Test
    @DisplayName("신규 사용자를 생성한다")
    void createNewUser_success() {
        // given
        OAuth2UserInfo userInfo = createMockUserInfo("social123", "test@example.com");
        SocialType socialType = SocialType.KAKAO;

        given(userRepository.existsByNickname(anyString())).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        User result = registrationService.createNewUser(userInfo, socialType);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSocialType()).isEqualTo(socialType);
        assertThat(result.getSocialId()).isEqualTo("social123");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getProfileImageUrl()).isEqualTo(DEFAULT_PROFILE_IMAGE_URL);
        assertThat(result.getLastLoginAt()).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("신규 사용자 생성 시 랜덤 닉네임을 생성한다")
    void createNewUser_generatesRandomNickname() {
        // given
        OAuth2UserInfo userInfo = createMockUserInfo("social123", "test@example.com");
        SocialType socialType = SocialType.GOOGLE;

        given(userRepository.existsByNickname(anyString())).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        User result = registrationService.createNewUser(userInfo, socialType);

        // then
        assertThat(result.getNickname()).isNotNull();
        assertThat(result.getNickname()).matches("^.+#\\d+$"); // "형용사명사#숫자" 패턴
    }

    @Test
    @DisplayName("닉네임 중복 시 새로운 닉네임을 생성한다")
    void createNewUser_retriesOnDuplicateNickname() {
        // given
        OAuth2UserInfo userInfo = createMockUserInfo("social123", "test@example.com");
        SocialType socialType = SocialType.NAVER;

        // 처음 2번은 중복, 3번째에 성공
        given(userRepository.existsByNickname(anyString()))
                .willReturn(true)
                .willReturn(true)
                .willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        User result = registrationService.createNewUser(userInfo, socialType);

        // then
        assertThat(result.getNickname()).isNotNull();
    }

    @Test
    @DisplayName("기존 사용자의 이메일을 업데이트한다")
    void updateExistingUser_updatesEmail() {
        // given
        User existingUser = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("social123")
                .nickname("테스트유저#1234")
                .email("old@example.com")
                .profileImageUrl(DEFAULT_PROFILE_IMAGE_URL)
                .build();

        OAuth2UserInfo userInfo = createMockUserInfo("social123", "new@example.com");

        // when
        User result = registrationService.updateExistingUser(existingUser, userInfo);

        // then
        assertThat(result.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("기존 사용자의 마지막 로그인 시간을 갱신한다")
    void updateExistingUser_updatesLastLoginAt() {
        // given
        User existingUser = User.builder()
                .socialType(SocialType.GOOGLE)
                .socialId("social123")
                .nickname("테스트유저#1234")
                .email("test@example.com")
                .profileImageUrl(DEFAULT_PROFILE_IMAGE_URL)
                .build();

        LocalDateTime beforeUpdate = existingUser.getLastLoginAt();
        OAuth2UserInfo userInfo = createMockUserInfo("social123", "test@example.com");

        // when
        User result = registrationService.updateExistingUser(existingUser, userInfo);

        // then
        assertThat(result.getLastLoginAt()).isNotNull();
        if (beforeUpdate != null) {
            assertThat(result.getLastLoginAt()).isAfterOrEqualTo(beforeUpdate);
        }
    }

    @Test
    @DisplayName("userInfo의 이메일이 null이면 기존 이메일을 유지한다")
    void updateExistingUser_keepsEmailWhenNull() {
        // given
        User existingUser = User.builder()
                .socialType(SocialType.APPLE)
                .socialId("social123")
                .nickname("테스트유저#1234")
                .email("original@example.com")
                .profileImageUrl(DEFAULT_PROFILE_IMAGE_URL)
                .build();

        OAuth2UserInfo userInfo = createMockUserInfo("social123", null);

        // when
        User result = registrationService.updateExistingUser(existingUser, userInfo);

        // then
        assertThat(result.getEmail()).isEqualTo("original@example.com");
    }

    private OAuth2UserInfo createMockUserInfo(String id, String email) {
        return new OAuth2UserInfo(Map.of()) {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public String getNickname() {
                return null;
            }

            @Override
            public String getEmail() {
                return email;
            }

            @Override
            public String getProfileImageUrl() {
                return null;
            }
        };
    }
}
