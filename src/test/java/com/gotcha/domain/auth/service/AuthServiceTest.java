package com.gotcha.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.gotcha.domain.auth.dto.TokenResponse;
import com.gotcha.domain.auth.exception.AuthException;
import com.gotcha.domain.auth.jwt.JwtTokenProvider;
import com.gotcha.domain.auth.oauth2.client.OAuth2Client;
import com.gotcha.domain.auth.oauth2.userinfo.OAuth2UserInfo;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private OAuth2Client oAuth2Client;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private OAuth2UserInfo oAuth2UserInfo;

    @Nested
    @DisplayName("login 메서드")
    class LoginTest {

        @Test
        @DisplayName("신규 사용자 로그인 성공")
        void login_newUser_success() {
            // given
            String provider = "kakao";
            String accessToken = "social-access-token";
            String socialId = "12345";

            given(oAuth2Client.getUserInfo(provider, accessToken)).willReturn(oAuth2UserInfo);
            given(oAuth2UserInfo.getId()).willReturn(socialId);
            given(oAuth2UserInfo.getProfileImageUrl()).willReturn("https://example.com/profile.jpg");
            given(userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, socialId))
                    .willReturn(Optional.empty());
            given(userRepository.existsByNickname(anyString())).willReturn(false);
            given(userRepository.save(any(User.class))).willAnswer(invocation -> {
                User user = invocation.getArgument(0);
                return User.builder()
                        .socialType(user.getSocialType())
                        .socialId(user.getSocialId())
                        .nickname(user.getNickname())
                        .profileImageUrl(user.getProfileImageUrl())
                        .isAnonymous(false)
                        .build();
            });
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("jwt-access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("jwt-refresh-token");

            // when
            TokenResponse response = authService.login(provider, accessToken);

            // then
            assertThat(response.accessToken()).isEqualTo("jwt-access-token");
            assertThat(response.refreshToken()).isEqualTo("jwt-refresh-token");
            assertThat(response.user().isNewUser()).isTrue();
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("기존 사용자 로그인 성공")
        void login_existingUser_success() {
            // given
            String provider = "kakao";
            String accessToken = "social-access-token";
            String socialId = "12345";

            User existingUser = User.builder()
                    .socialType(SocialType.KAKAO)
                    .socialId(socialId)
                    .nickname("기존유저#123")
                    .profileImageUrl("https://old.com/profile.jpg")
                    .isAnonymous(false)
                    .build();

            given(oAuth2Client.getUserInfo(provider, accessToken)).willReturn(oAuth2UserInfo);
            given(oAuth2UserInfo.getId()).willReturn(socialId);
            given(oAuth2UserInfo.getProfileImageUrl()).willReturn("https://new.com/profile.jpg");
            given(userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, socialId))
                    .willReturn(Optional.of(existingUser));
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("jwt-access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("jwt-refresh-token");

            // when
            TokenResponse response = authService.login(provider, accessToken);

            // then
            assertThat(response.accessToken()).isEqualTo("jwt-access-token");
            assertThat(response.refreshToken()).isEqualTo("jwt-refresh-token");
            assertThat(response.user().isNewUser()).isFalse();
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("지원하지 않는 소셜 타입으로 로그인 시도 시 실패")
        void login_unsupportedProvider_fails() {
            // given
            String provider = "facebook";
            String accessToken = "social-access-token";

            // when & then
            assertThatThrownBy(() -> authService.login(provider, accessToken))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("소셜 ID가 null일 경우 실패")
        void login_nullSocialId_fails() {
            // given
            String provider = "kakao";
            String accessToken = "social-access-token";

            given(oAuth2Client.getUserInfo(provider, accessToken)).willReturn(oAuth2UserInfo);
            given(oAuth2UserInfo.getId()).willReturn(null);

            // when & then
            assertThatThrownBy(() -> authService.login(provider, accessToken))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("Google 프로바이더로 로그인 성공")
        void login_google_success() {
            // given
            String provider = "google";
            String accessToken = "google-access-token";
            String socialId = "google-12345";

            given(oAuth2Client.getUserInfo(provider, accessToken)).willReturn(oAuth2UserInfo);
            given(oAuth2UserInfo.getId()).willReturn(socialId);
            given(oAuth2UserInfo.getProfileImageUrl()).willReturn(null);
            given(userRepository.findBySocialTypeAndSocialId(SocialType.GOOGLE, socialId))
                    .willReturn(Optional.empty());
            given(userRepository.existsByNickname(anyString())).willReturn(false);
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("jwt-access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("jwt-refresh-token");

            // when
            TokenResponse response = authService.login(provider, accessToken);

            // then
            assertThat(response.accessToken()).isNotNull();
            assertThat(response.user().isNewUser()).isTrue();
        }

        @Test
        @DisplayName("Naver 프로바이더로 로그인 성공")
        void login_naver_success() {
            // given
            String provider = "naver";
            String accessToken = "naver-access-token";
            String socialId = "naver-12345";

            given(oAuth2Client.getUserInfo(provider, accessToken)).willReturn(oAuth2UserInfo);
            given(oAuth2UserInfo.getId()).willReturn(socialId);
            given(oAuth2UserInfo.getProfileImageUrl()).willReturn(null);
            given(userRepository.findBySocialTypeAndSocialId(SocialType.NAVER, socialId))
                    .willReturn(Optional.empty());
            given(userRepository.existsByNickname(anyString())).willReturn(false);
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("jwt-access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("jwt-refresh-token");

            // when
            TokenResponse response = authService.login(provider, accessToken);

            // then
            assertThat(response.accessToken()).isNotNull();
            assertThat(response.user().isNewUser()).isTrue();
        }
    }
}
