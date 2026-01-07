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
import com.gotcha.domain.auth.oauth2.userinfo.KakaoOAuth2UserInfo;
import com.gotcha.domain.auth.oauth2.userinfo.OAuth2UserInfo;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.HashMap;
import java.util.Map;
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
    private OAuth2Client oauth2Client;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Nested
    @DisplayName("login 메서드")
    class LoginTest {

        @Test
        @DisplayName("신규 사용자 로그인 - 사용자 생성 후 토큰 발급")
        void login_newUser_createsUserAndReturnsToken() {
            // given
            String provider = "kakao";
            String accessToken = "valid-access-token";
            OAuth2UserInfo userInfo = createKakaoUserInfo("kakao-123", "테스트유저");

            given(oauth2Client.getUserInfo(provider, accessToken)).willReturn(userInfo);
            given(userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, "kakao-123"))
                    .willReturn(Optional.empty());
            given(userRepository.existsByNickname(anyString())).willReturn(false);
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("jwt-access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("jwt-refresh-token");

            // when
            TokenResponse result = authService.login(provider, accessToken);

            // then
            assertThat(result.accessToken()).isEqualTo("jwt-access-token");
            assertThat(result.refreshToken()).isEqualTo("jwt-refresh-token");
            assertThat(result.user().isNewUser()).isTrue();
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("기존 사용자 로그인 - 프로필 업데이트 후 토큰 발급")
        void login_existingUser_updatesAndReturnsToken() {
            // given
            String provider = "kakao";
            String accessToken = "valid-access-token";
            OAuth2UserInfo userInfo = createKakaoUserInfo("kakao-123", "테스트유저");

            User existingUser = User.builder()
                    .socialType(SocialType.KAKAO)
                    .socialId("kakao-123")
                    .nickname("기존닉네임#1")
                    .profileImageUrl("old-image-url")
                    .isAnonymous(false)
                    .build();

            given(oauth2Client.getUserInfo(provider, accessToken)).willReturn(userInfo);
            given(userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, "kakao-123"))
                    .willReturn(Optional.of(existingUser));
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("jwt-access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("jwt-refresh-token");

            // when
            TokenResponse result = authService.login(provider, accessToken);

            // then
            assertThat(result.accessToken()).isEqualTo("jwt-access-token");
            assertThat(result.refreshToken()).isEqualTo("jwt-refresh-token");
            assertThat(result.user().isNewUser()).isFalse();
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("소셜 ID가 null인 경우 - 예외 발생")
        void login_nullSocialId_throwsException() {
            // given
            String provider = "kakao";
            String accessToken = "valid-access-token";
            OAuth2UserInfo userInfo = createKakaoUserInfo(null, "테스트유저");

            given(oauth2Client.getUserInfo(provider, accessToken)).willReturn(userInfo);

            // when & then
            assertThatThrownBy(() -> authService.login(provider, accessToken))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("소셜 ID가 빈 문자열인 경우 - 예외 발생")
        void login_blankSocialId_throwsException() {
            // given
            String provider = "kakao";
            String accessToken = "valid-access-token";
            OAuth2UserInfo userInfo = createKakaoUserInfo("   ", "테스트유저");

            given(oauth2Client.getUserInfo(provider, accessToken)).willReturn(userInfo);

            // when & then
            assertThatThrownBy(() -> authService.login(provider, accessToken))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("지원하지 않는 소셜 타입 - 예외 발생")
        void login_unsupportedProvider_throwsException() {
            // given
            String provider = "facebook";
            String accessToken = "valid-access-token";

            given(oauth2Client.getUserInfo(provider, accessToken))
                    .willThrow(AuthException.unsupportedSocialType());

            // when & then
            assertThatThrownBy(() -> authService.login(provider, accessToken))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("OAuth2 API 호출 실패 - 예외 발생")
        void login_oauth2ApiFailed_throwsException() {
            // given
            String provider = "kakao";
            String accessToken = "invalid-access-token";

            given(oauth2Client.getUserInfo(provider, accessToken))
                    .willThrow(AuthException.socialLoginFailed());

            // when & then
            assertThatThrownBy(() -> authService.login(provider, accessToken))
                    .isInstanceOf(AuthException.class);
        }
    }

    @Nested
    @DisplayName("닉네임 생성 엣지 케이스")
    class NicknameGenerationEdgeCaseTest {

        @Test
        @DisplayName("닉네임 중복 시 최대 10번까지 재시도")
        void login_nicknameCollision_retriesUpToMaxAttempts() {
            // given
            String provider = "kakao";
            String accessToken = "valid-access-token";
            OAuth2UserInfo userInfo = createKakaoUserInfo("kakao-999", "테스트유저");

            given(oauth2Client.getUserInfo(provider, accessToken)).willReturn(userInfo);
            given(userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, "kakao-999"))
                    .willReturn(Optional.empty());
            // 처음 9번은 중복, 10번째에 성공
            given(userRepository.existsByNickname(anyString()))
                    .willReturn(true, true, true, true, true, true, true, true, true, false);
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("jwt-access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("jwt-refresh-token");

            // when
            TokenResponse result = authService.login(provider, accessToken);

            // then
            assertThat(result.accessToken()).isEqualTo("jwt-access-token");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("10번 모두 중복 시 fallback 닉네임 사용")
        void login_allNicknamesCollide_usesFallbackNickname() {
            // given
            String provider = "kakao";
            String accessToken = "valid-access-token";
            OAuth2UserInfo userInfo = createKakaoUserInfo("kakao-888", "테스트유저");

            given(oauth2Client.getUserInfo(provider, accessToken)).willReturn(userInfo);
            given(userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, "kakao-888"))
                    .willReturn(Optional.empty());
            // 10번 모두 중복
            given(userRepository.existsByNickname(anyString())).willReturn(true);
            given(userRepository.save(any(User.class))).willAnswer(invocation -> {
                User savedUser = invocation.getArgument(0);
                // fallback 닉네임 패턴 검증: "가챠유저#숫자"
                assertThat(savedUser.getNickname()).matches("^가챠유저#\\d+$");
                return savedUser;
            });
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("jwt-access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("jwt-refresh-token");

            // when
            TokenResponse result = authService.login(provider, accessToken);

            // then
            assertThat(result.accessToken()).isEqualTo("jwt-access-token");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("닉네임 중복 체크와 저장 사이 Race Condition 시나리오 - DataIntegrityViolation 발생 가능")
        void login_raceCondition_potentialDataIntegrityViolation() {
            // given - 이 테스트는 Race Condition 가능성을 문서화
            // 실제 Race Condition은 통합 테스트에서 검증 필요
            String provider = "kakao";
            String accessToken = "valid-access-token";
            OAuth2UserInfo userInfo = createKakaoUserInfo("kakao-777", "테스트유저");

            given(oauth2Client.getUserInfo(provider, accessToken)).willReturn(userInfo);
            given(userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, "kakao-777"))
                    .willReturn(Optional.empty());
            // existsByNickname은 false 반환 (중복 없음)
            given(userRepository.existsByNickname(anyString())).willReturn(false);
            // 하지만 save 시점에 다른 트랜잭션이 먼저 저장했다면 예외 발생 가능
            // 현재 코드는 이를 처리하지 않음 - 잠재적 버그
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("jwt-access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("jwt-refresh-token");

            // when
            TokenResponse result = authService.login(provider, accessToken);

            // then - 현재는 정상 동작하지만, DB unique 제약 추가 시 예외 발생 가능
            assertThat(result).isNotNull();
        }
    }

    private OAuth2UserInfo createKakaoUserInfo(String id, String nickname) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("nickname", nickname);
        profile.put("profile_image_url", "https://example.com/profile.jpg");

        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("profile", profile);

        Map<String, Object> attributes = new HashMap<>();
        if (id != null && !id.isBlank()) {
            attributes.put("id", Long.parseLong(id.replace("kakao-", "")));
        } else if (id != null) {
            // blank string case - id를 아예 넣지 않거나 null로
            attributes.put("id", null);
        }
        attributes.put("kakao_account", kakaoAccount);

        return new KakaoOAuth2UserInfo(attributes) {
            @Override
            public String getId() {
                return id;
            }
        };
    }
}
