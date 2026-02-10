package com.gotcha.domain.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.gotcha.domain.auth.exception.AuthErrorCode;
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

    @Mock
    private InMemoryAuthorizationRequestRepository authorizationRequestRepository;

    @BeforeEach
    void setUp() {
        handler = new OAuth2AuthenticationFailureHandler(authorizationRequestRepository);
        ReflectionTestUtils.setField(handler, "defaultRedirectUri", "http://localhost:3000/oauth/callback");
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
            assertThat(redirectUrl).contains("error=" + AuthErrorCode.SOCIAL_LOGIN_FAILED.getCode());
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
            assertThat(redirectUrl).contains("error=" + AuthErrorCode.OAUTH_ACCESS_DENIED.getCode());
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
            assertThat(redirectUrl).contains("error=" + AuthErrorCode.OAUTH_INVALID_TOKEN.getCode());
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
            assertThat(redirectUrl).contains("error=" + AuthErrorCode.SOCIAL_LOGIN_FAILED.getCode());
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
            assertThat(redirectUrl).contains("error=");
        }

        @Test
        @DisplayName("에러 코드만 포함하고 메시지는 포함하지 않음")
        void onAuthenticationFailure_containsOnlyErrorCode_noMessage() throws Exception {
            // given
            OAuth2Error error = new OAuth2Error("access_denied", "User cancelled", null);
            OAuth2AuthenticationException exception = new OAuth2AuthenticationException(error);

            // when
            handler.onAuthenticationFailure(request, response, exception);

            // then
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(redirectStrategy).sendRedirect(any(), any(), urlCaptor.capture());

            String redirectUrl = urlCaptor.getValue();
            assertThat(redirectUrl).contains("error=");
            assertThat(redirectUrl).doesNotContain("message=");
        }
    }
}
