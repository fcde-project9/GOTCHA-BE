package com.gotcha.domain.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.gotcha.domain.auth.exception.AuthErrorCode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationFailureHandlerTest {

    private OAuth2AuthenticationFailureHandler handler;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private RedirectStrategy redirectStrategy;

    @BeforeEach
    void setUp() {
        handler = new OAuth2AuthenticationFailureHandler();
        ReflectionTestUtils.setField(handler, "redirectUri", "http://localhost:3000/oauth/callback");
        handler.setRedirectStrategy(redirectStrategy);
    }

    @Nested
    @DisplayName("인증 실패 처리")
    class OnAuthenticationFailureTest {

        @Test
        @DisplayName("일반 AuthenticationException 발생 시 기본 에러 코드 사용")
        void onAuthenticationFailure_withGenericException_usesDefaultErrorCode() throws Exception {
            // given
            AuthenticationException exception = new AuthenticationException("Internal error details") {};

            // when
            handler.onAuthenticationFailure(request, response, exception);

            // then
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(redirectStrategy).sendRedirect(any(), any(), urlCaptor.capture());

            String redirectUrl = urlCaptor.getValue();
            String expectedMessage = URLEncoder.encode(
                    AuthErrorCode.SOCIAL_LOGIN_FAILED.getMessage(), StandardCharsets.UTF_8);
            assertThat(redirectUrl).contains("code=" + AuthErrorCode.SOCIAL_LOGIN_FAILED.getCode());
            assertThat(redirectUrl).contains("message=" + expectedMessage);
            // 내부 에러 메시지가 노출되지 않음을 확인
            assertThat(redirectUrl).doesNotContain("Internal");
        }

        @Test
        @DisplayName("OAuth2AuthenticationException(access_denied) 발생 시 OAUTH_ACCESS_DENIED 에러 코드 사용")
        void onAuthenticationFailure_withAccessDenied_usesAccessDeniedErrorCode() throws Exception {
            // given
            OAuth2Error error = new OAuth2Error("access_denied", "User cancelled the login", null);
            OAuth2AuthenticationException exception = new OAuth2AuthenticationException(error);

            // when
            handler.onAuthenticationFailure(request, response, exception);

            // then
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(redirectStrategy).sendRedirect(any(), any(), urlCaptor.capture());

            String redirectUrl = urlCaptor.getValue();
            String expectedMessage = URLEncoder.encode(
                    AuthErrorCode.OAUTH_ACCESS_DENIED.getMessage(), StandardCharsets.UTF_8);
            assertThat(redirectUrl).contains("code=" + AuthErrorCode.OAUTH_ACCESS_DENIED.getCode());
            assertThat(redirectUrl).contains("message=" + expectedMessage);
        }

        @Test
        @DisplayName("OAuth2AuthenticationException(invalid_token) 발생 시 OAUTH_INVALID_TOKEN 에러 코드 사용")
        void onAuthenticationFailure_withInvalidToken_usesInvalidTokenErrorCode() throws Exception {
            // given
            OAuth2Error error = new OAuth2Error("invalid_token", "The token has expired", null);
            OAuth2AuthenticationException exception = new OAuth2AuthenticationException(error);

            // when
            handler.onAuthenticationFailure(request, response, exception);

            // then
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(redirectStrategy).sendRedirect(any(), any(), urlCaptor.capture());

            String redirectUrl = urlCaptor.getValue();
            String expectedMessage = URLEncoder.encode(
                    AuthErrorCode.OAUTH_INVALID_TOKEN.getMessage(), StandardCharsets.UTF_8);
            assertThat(redirectUrl).contains("code=" + AuthErrorCode.OAUTH_INVALID_TOKEN.getCode());
            assertThat(redirectUrl).contains("message=" + expectedMessage);
        }

        @Test
        @DisplayName("OAuth2AuthenticationException(unknown) 발생 시 기본 에러 코드 사용")
        void onAuthenticationFailure_withUnknownOAuth2Error_usesDefaultErrorCode() throws Exception {
            // given
            OAuth2Error error = new OAuth2Error("invalid_grant", "The authorization code has expired", null);
            OAuth2AuthenticationException exception = new OAuth2AuthenticationException(error);

            // when
            handler.onAuthenticationFailure(request, response, exception);

            // then
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(redirectStrategy).sendRedirect(any(), any(), urlCaptor.capture());

            String redirectUrl = urlCaptor.getValue();
            String expectedMessage = URLEncoder.encode(
                    AuthErrorCode.SOCIAL_LOGIN_FAILED.getMessage(), StandardCharsets.UTF_8);
            assertThat(redirectUrl).contains("code=" + AuthErrorCode.SOCIAL_LOGIN_FAILED.getCode());
            assertThat(redirectUrl).contains("message=" + expectedMessage);
            // OAuth2 표준 에러 상세 메시지가 노출되지 않음을 확인
            assertThat(redirectUrl).doesNotContain("authorization");
        }

        @Test
        @DisplayName("리다이렉트 URL 형식이 올바름")
        void onAuthenticationFailure_redirectUrl_hasCorrectFormat() throws Exception {
            // given
            AuthenticationException exception = new AuthenticationException("Error") {};

            // when
            handler.onAuthenticationFailure(request, response, exception);

            // then
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(redirectStrategy).sendRedirect(any(), any(), urlCaptor.capture());

            String redirectUrl = urlCaptor.getValue();
            assertThat(redirectUrl).startsWith("http://localhost:3000/oauth/callback?");
            assertThat(redirectUrl).contains("code=");
            assertThat(redirectUrl).contains("message=");
        }

        @Test
        @DisplayName("한글 메시지가 올바르게 URL 인코딩됨")
        void onAuthenticationFailure_koreanMessage_isProperlyEncoded() throws Exception {
            // given
            OAuth2Error error = new OAuth2Error("access_denied", "User cancelled", null);
            OAuth2AuthenticationException exception = new OAuth2AuthenticationException(error);

            // when
            handler.onAuthenticationFailure(request, response, exception);

            // then
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(redirectStrategy).sendRedirect(any(), any(), urlCaptor.capture());

            String redirectUrl = urlCaptor.getValue();
            String expectedMessage = AuthErrorCode.OAUTH_ACCESS_DENIED.getMessage();
            String expectedEncoded = URLEncoder.encode(expectedMessage, StandardCharsets.UTF_8);

            assertThat(redirectUrl).contains("message=" + expectedEncoded);
            // 한글이 퍼센트 인코딩되었는지 확인 (원본 한글이 URL에 직접 포함되지 않음)
            assertThat(redirectUrl).doesNotContain(expectedMessage);
            // 인코딩된 형태(%XX)가 포함되어 있는지 확인
            assertThat(expectedEncoded).containsPattern("%[0-9A-F]{2}");
        }
    }
}
