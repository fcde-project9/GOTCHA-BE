package com.gotcha.domain.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.gotcha.domain.auth.jwt.JwtTokenProvider;
import com.gotcha.domain.auth.service.AuthService;
import com.gotcha.domain.auth.service.OAuthTokenCookieService;
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
    private OAuthTokenCookieService oAuthTokenCacheService;

    @Mock
    private HttpCookieOAuth2AuthorizationRequestRepository cookieRepository;

    @Mock
    private Authentication authentication;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        ReflectionTestUtils.setField(successHandler, "defaultRedirectUri", "http://localhost:3000/oauth/callback");
    }

    @Nested
    @DisplayName("신규 사용자 로그인")
    class NewUserLogin {

        @Test
        @DisplayName("신규 사용자 로그인 - 임시 코드만 전달 (isNewUser는 토큰 교환 응답에 포함)")
        void newUserLogin_redirectWithTempCodeOnly() throws Exception {
            // given
            User user = createTestUser(SocialType.KAKAO, "test@kakao.com");
            CustomOAuth2User oAuth2User = new CustomOAuth2User(user, new HashMap<>(), true);
            String tempCode = "temp-code-uuid";

            given(authentication.getPrincipal()).willReturn(oAuth2User);
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("refresh-token");
            given(oAuthTokenCacheService.storeTokens(anyString(), anyString(), anyBoolean(),
                    any(HttpServletRequest.class), any(HttpServletResponse.class)))
                    .willReturn(tempCode);

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // then
            String redirectUrl = response.getRedirectedUrl();
            assertThat(redirectUrl).isNotNull();
            assertThat(redirectUrl).contains("code=" + tempCode);
            assertThat(redirectUrl).doesNotContain("isNewUser=");
            assertThat(redirectUrl).doesNotContain("accessToken=");
            assertThat(redirectUrl).doesNotContain("refreshToken=");
            verify(authService).saveRefreshToken(any(User.class), eq("refresh-token"));
            verify(oAuthTokenCacheService).storeTokens(eq("access-token"), eq("refresh-token"), eq(true),
                    any(HttpServletRequest.class), any(HttpServletResponse.class));
        }
    }

    @Nested
    @DisplayName("기존 사용자 로그인")
    class ExistingUserLogin {

        @Test
        @DisplayName("기존 사용자 로그인 - 임시 코드만 전달 (isNewUser는 토큰 교환 응답에 포함)")
        void existingUserLogin_redirectWithTempCodeOnly() throws Exception {
            // given
            User user = createTestUser(SocialType.GOOGLE, "test@gmail.com");
            CustomOAuth2User oAuth2User = new CustomOAuth2User(user, new HashMap<>(), false);
            String tempCode = "temp-code-existing-user";

            given(authentication.getPrincipal()).willReturn(oAuth2User);
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("refresh-token");
            given(oAuthTokenCacheService.storeTokens(anyString(), anyString(), anyBoolean(),
                    any(HttpServletRequest.class), any(HttpServletResponse.class)))
                    .willReturn(tempCode);

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // then
            String redirectUrl = response.getRedirectedUrl();
            assertThat(redirectUrl).isNotNull();
            assertThat(redirectUrl).contains("code=" + tempCode);
            assertThat(redirectUrl).doesNotContain("isNewUser=");
            assertThat(redirectUrl).doesNotContain("accessToken=");
            assertThat(redirectUrl).doesNotContain("refreshToken=");
            verify(authService).saveRefreshToken(any(User.class), eq("refresh-token"));
            verify(oAuthTokenCacheService).storeTokens(eq("access-token"), eq("refresh-token"), eq(false),
                    any(HttpServletRequest.class), any(HttpServletResponse.class));
        }
    }

    @Nested
    @DisplayName("소셜 타입별 로그인")
    class SocialTypeLogin {

        @Test
        @DisplayName("카카오 로그인 - 정상 리다이렉트 (임시 코드 사용)")
        void kakaoLogin_redirectSuccess() throws Exception {
            // given
            User user = createTestUser(SocialType.KAKAO, "test@kakao.com");
            CustomOAuth2User oAuth2User = new CustomOAuth2User(user, createKakaoAttributes(), true);
            String tempCode = "kakao-temp-code";

            given(authentication.getPrincipal()).willReturn(oAuth2User);
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("kakao-access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("kakao-refresh-token");
            given(oAuthTokenCacheService.storeTokens(eq("kakao-access-token"), eq("kakao-refresh-token"), eq(true),
                    any(HttpServletRequest.class), any(HttpServletResponse.class)))
                    .willReturn(tempCode);

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // then
            assertThat(response.getStatus()).isEqualTo(302);
            assertThat(response.getRedirectedUrl()).contains("http://localhost:3000/oauth/callback");
            assertThat(response.getRedirectedUrl()).contains("code=" + tempCode);
            verify(authService).saveRefreshToken(any(User.class), eq("kakao-refresh-token"));
        }

        @Test
        @DisplayName("구글 로그인 - 정상 리다이렉트 (임시 코드 사용)")
        void googleLogin_redirectSuccess() throws Exception {
            // given
            User user = createTestUser(SocialType.GOOGLE, "test@gmail.com");
            CustomOAuth2User oAuth2User = new CustomOAuth2User(user, createGoogleAttributes(), true);
            String tempCode = "google-temp-code";

            given(authentication.getPrincipal()).willReturn(oAuth2User);
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("google-access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("google-refresh-token");
            given(oAuthTokenCacheService.storeTokens(eq("google-access-token"), eq("google-refresh-token"), eq(true),
                    any(HttpServletRequest.class), any(HttpServletResponse.class)))
                    .willReturn(tempCode);

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // then
            assertThat(response.getStatus()).isEqualTo(302);
            assertThat(response.getRedirectedUrl()).contains("code=" + tempCode);
            assertThat(response.getRedirectedUrl()).doesNotContain("accessToken=");
            verify(authService).saveRefreshToken(any(User.class), eq("google-refresh-token"));
        }

        @Test
        @DisplayName("네이버 로그인 - 정상 리다이렉트 (임시 코드 사용)")
        void naverLogin_redirectSuccess() throws Exception {
            // given
            User user = createTestUser(SocialType.NAVER, "test@naver.com");
            CustomOAuth2User oAuth2User = new CustomOAuth2User(user, createNaverAttributes(), true);
            String tempCode = "naver-temp-code";

            given(authentication.getPrincipal()).willReturn(oAuth2User);
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("naver-access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("naver-refresh-token");
            given(oAuthTokenCacheService.storeTokens(eq("naver-access-token"), eq("naver-refresh-token"), eq(true),
                    any(HttpServletRequest.class), any(HttpServletResponse.class)))
                    .willReturn(tempCode);

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // then
            assertThat(response.getStatus()).isEqualTo(302);
            assertThat(response.getRedirectedUrl()).contains("code=" + tempCode);
            assertThat(response.getRedirectedUrl()).doesNotContain("accessToken=");
            verify(authService).saveRefreshToken(any(User.class), eq("naver-refresh-token"));
        }
    }

    @Nested
    @DisplayName("email이 null인 경우")
    class EmailNullCase {

        @Test
        @DisplayName("email 미제공 시 - 정상 리다이렉트 (임시 코드 사용)")
        void emailNull_redirectSuccess() throws Exception {
            // given
            User user = createTestUser(SocialType.KAKAO, null);
            CustomOAuth2User oAuth2User = new CustomOAuth2User(user, new HashMap<>(), true);
            String tempCode = "temp-code-no-email";

            given(authentication.getPrincipal()).willReturn(oAuth2User);
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("refresh-token");
            given(oAuthTokenCacheService.storeTokens(eq("access-token"), eq("refresh-token"), eq(true),
                    any(HttpServletRequest.class), any(HttpServletResponse.class)))
                    .willReturn(tempCode);

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // then
            assertThat(response.getStatus()).isEqualTo(302);
            assertThat(response.getRedirectedUrl()).isNotNull();
            assertThat(response.getRedirectedUrl()).contains("code=" + tempCode);
            verify(authService).saveRefreshToken(any(User.class), eq("refresh-token"));
        }
    }

    @Nested
    @DisplayName("redirect_uri 쿠키 사용")
    class RedirectUriCookie {

        @Test
        @DisplayName("쿠키에 redirect_uri 있으면 해당 URI로 리다이렉트 (임시 코드 사용)")
        void redirectUri_fromCookie() throws Exception {
            // given
            User user = createTestUser(SocialType.KAKAO, "test@kakao.com");
            CustomOAuth2User oAuth2User = new CustomOAuth2User(user, new HashMap<>(), true);
            String customRedirectUri = "https://custom.gotcha.com/oauth/callback";
            String tempCode = "temp-code-custom-uri";

            given(authentication.getPrincipal()).willReturn(oAuth2User);
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("refresh-token");
            given(oAuthTokenCacheService.storeTokens(eq("access-token"), eq("refresh-token"), eq(true),
                    any(HttpServletRequest.class), any(HttpServletResponse.class)))
                    .willReturn(tempCode);
            given(cookieRepository.getRedirectUriFromCookie(request)).willReturn(customRedirectUri);

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // then
            String redirectUrl = response.getRedirectedUrl();
            assertThat(redirectUrl).startsWith(customRedirectUri);
            assertThat(redirectUrl).contains("code=" + tempCode);
            assertThat(redirectUrl).doesNotContain("accessToken=");
            verify(cookieRepository).removeRedirectUriCookie(response);
        }

        @Test
        @DisplayName("쿠키에 redirect_uri 없으면 기본값 사용")
        void redirectUri_useDefault() throws Exception {
            // given
            User user = createTestUser(SocialType.KAKAO, "test@kakao.com");
            CustomOAuth2User oAuth2User = new CustomOAuth2User(user, new HashMap<>(), true);
            String tempCode = "temp-code-default-uri";

            given(authentication.getPrincipal()).willReturn(oAuth2User);
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("refresh-token");
            given(oAuthTokenCacheService.storeTokens(eq("access-token"), eq("refresh-token"), eq(true),
                    any(HttpServletRequest.class), any(HttpServletResponse.class)))
                    .willReturn(tempCode);
            given(cookieRepository.getRedirectUriFromCookie(request)).willReturn(null);

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // then
            String redirectUrl = response.getRedirectedUrl();
            assertThat(redirectUrl).startsWith("http://localhost:3000/oauth/callback");
            assertThat(redirectUrl).contains("code=" + tempCode);
            verify(cookieRepository).removeRedirectUriCookie(response);
        }

        @Test
        @DisplayName("쿠키에 빈 문자열 redirect_uri면 기본값 사용")
        void redirectUri_blankUseDefault() throws Exception {
            // given
            User user = createTestUser(SocialType.KAKAO, "test@kakao.com");
            CustomOAuth2User oAuth2User = new CustomOAuth2User(user, new HashMap<>(), true);
            String tempCode = "temp-code-blank-uri";

            given(authentication.getPrincipal()).willReturn(oAuth2User);
            given(jwtTokenProvider.generateAccessToken(any(User.class))).willReturn("access-token");
            given(jwtTokenProvider.generateRefreshToken(any(User.class))).willReturn("refresh-token");
            given(oAuthTokenCacheService.storeTokens(eq("access-token"), eq("refresh-token"), eq(true),
                    any(HttpServletRequest.class), any(HttpServletResponse.class)))
                    .willReturn(tempCode);
            given(cookieRepository.getRedirectUriFromCookie(request)).willReturn("   ");

            // when
            successHandler.onAuthenticationSuccess(request, response, authentication);

            // then
            String redirectUrl = response.getRedirectedUrl();
            assertThat(redirectUrl).startsWith("http://localhost:3000/oauth/callback");
            assertThat(redirectUrl).contains("code=" + tempCode);
        }
    }

    private User createTestUser(SocialType socialType, String email) {
        User user = User.builder()
                .socialType(socialType)
                .socialId("test-social-id")
                .nickname("테스트유저#1")
                .email(email)
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
