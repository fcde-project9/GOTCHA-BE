package com.gotcha.domain.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.gotcha.domain.auth.jwt.JwtTokenProvider;
import com.gotcha.domain.auth.service.AuthService;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler successHandler;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthService authService;

    @Mock
    private Authentication authentication;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        ReflectionTestUtils.setField(successHandler, "redirectUri", "http://localhost:3000/oauth/callback");
    }

    @Nested
    @DisplayName("신규 사용자 로그인")
    class NewUserLogin {

        @Test
        @DisplayName("신규 사용자 로그인 - isNewUser=true 파라미터 포함")
        void newUserLogin_redirectWithIsNewUserTrue() throws Exception {
            // given
            User user = createTestUser(SocialType.KAKAO, "test@kakao.com");
            CustomOAuth2User oAuth2User = new CustomOAuth2User(user, new HashMap<>(), true);

            given(authentication.getPrincipal()).willReturn(oAuth2User);
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("refresh-token");

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // then
            String redirectUrl = response.getRedirectedUrl();
            assertThat(redirectUrl).isNotNull();
            assertThat(redirectUrl).contains("isNewUser=true");
            assertThat(redirectUrl).contains("accessToken=access-token");
            assertThat(redirectUrl).contains("refreshToken=refresh-token");
            verify(authService).saveRefreshToken(any(User.class), eq("refresh-token"));
        }
    }

    @Nested
    @DisplayName("기존 사용자 로그인")
    class ExistingUserLogin {

        @Test
        @DisplayName("기존 사용자 로그인 - isNewUser=false 파라미터 포함")
        void existingUserLogin_redirectWithIsNewUserFalse() throws Exception {
            // given
            User user = createTestUser(SocialType.GOOGLE, "test@gmail.com");
            CustomOAuth2User oAuth2User = new CustomOAuth2User(user, new HashMap<>(), false);

            given(authentication.getPrincipal()).willReturn(oAuth2User);
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("refresh-token");

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // then
            String redirectUrl = response.getRedirectedUrl();
            assertThat(redirectUrl).isNotNull();
            assertThat(redirectUrl).contains("isNewUser=false");
            verify(authService).saveRefreshToken(any(User.class), eq("refresh-token"));
        }
    }

    @Nested
    @DisplayName("소셜 타입별 로그인")
    class SocialTypeLogin {

        @Test
        @DisplayName("카카오 로그인 - 정상 리다이렉트")
        void kakaoLogin_redirectSuccess() throws Exception {
            // given
            User user = createTestUser(SocialType.KAKAO, "test@kakao.com");
            CustomOAuth2User oAuth2User = new CustomOAuth2User(user, createKakaoAttributes(), true);

            given(authentication.getPrincipal()).willReturn(oAuth2User);
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("kakao-access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("kakao-refresh-token");

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // then
            assertThat(response.getStatus()).isEqualTo(302);
            assertThat(response.getRedirectedUrl()).contains("http://localhost:3000/oauth/callback");
            verify(authService).saveRefreshToken(any(User.class), eq("kakao-refresh-token"));
        }

        @Test
        @DisplayName("구글 로그인 - 정상 리다이렉트")
        void googleLogin_redirectSuccess() throws Exception {
            // given
            User user = createTestUser(SocialType.GOOGLE, "test@gmail.com");
            CustomOAuth2User oAuth2User = new CustomOAuth2User(user, createGoogleAttributes(), true);

            given(authentication.getPrincipal()).willReturn(oAuth2User);
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("google-access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("google-refresh-token");

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // then
            assertThat(response.getStatus()).isEqualTo(302);
            assertThat(response.getRedirectedUrl()).contains("accessToken=google-access-token");
            verify(authService).saveRefreshToken(any(User.class), eq("google-refresh-token"));
        }

        @Test
        @DisplayName("네이버 로그인 - 정상 리다이렉트")
        void naverLogin_redirectSuccess() throws Exception {
            // given
            User user = createTestUser(SocialType.NAVER, "test@naver.com");
            CustomOAuth2User oAuth2User = new CustomOAuth2User(user, createNaverAttributes(), true);

            given(authentication.getPrincipal()).willReturn(oAuth2User);
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("naver-access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("naver-refresh-token");

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // then
            assertThat(response.getStatus()).isEqualTo(302);
            assertThat(response.getRedirectedUrl()).contains("accessToken=naver-access-token");
            verify(authService).saveRefreshToken(any(User.class), eq("naver-refresh-token"));
        }
    }

    @Nested
    @DisplayName("email이 null인 경우")
    class EmailNullCase {

        @Test
        @DisplayName("email 미제공 시 - 정상 리다이렉트")
        void emailNull_redirectSuccess() throws Exception {
            // given
            User user = createTestUser(SocialType.KAKAO, null);
            CustomOAuth2User oAuth2User = new CustomOAuth2User(user, new HashMap<>(), true);

            given(authentication.getPrincipal()).willReturn(oAuth2User);
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("refresh-token");

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // then
            assertThat(response.getStatus()).isEqualTo(302);
            assertThat(response.getRedirectedUrl()).isNotNull();
            verify(authService).saveRefreshToken(any(User.class), eq("refresh-token"));
        }
    }

    private User createTestUser(SocialType socialType, String email) {
        User user = User.builder()
                .socialType(socialType)
                .socialId("test-social-id")
                .nickname("테스트유저#1")
                .email(email)
                .isAnonymous(false)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private Map<String, Object> createKakaoAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345L);
        return attributes;
    }

    private Map<String, Object> createGoogleAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "google-123");
        attributes.put("email", "test@gmail.com");
        return attributes;
    }

    private Map<String, Object> createNaverAttributes() {
        Map<String, Object> response = new HashMap<>();
        response.put("id", "naver-123");
        response.put("email", "test@naver.com");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("response", response);
        return attributes;
    }
}
