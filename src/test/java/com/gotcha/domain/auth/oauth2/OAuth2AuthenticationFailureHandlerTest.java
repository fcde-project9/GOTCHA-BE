package com.gotcha.domain.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.gotcha.domain.auth.exception.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
        @DisplayName("일반 AuthenticationException 발생 시 기본 에러 메시지 사용")
        void onAuthenticationFailure_withGenericException_usesDefaultMessage() throws Exception {
            // given
            AuthenticationException exception = new AuthenticationException("Internal error details") {};

            // when
            handler.onAuthenticationFailure(request, response, exception);

            // then
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(redirectStrategy).sendRedirect(any(), any(), urlCaptor.capture());

            String redirectUrl = urlCaptor.getValue();
            String expectedEncodedMessage = URLEncoder.encode("소셜 로그인에 실패했습니다", StandardCharsets.UTF_8);
            assertThat(redirectUrl).contains("error=" + expectedEncodedMessage);
            // 내부 에러 메시지가 노출되지 않음을 확인
            assertThat(redirectUrl).doesNotContain("Internal");
        }

        @Test
        @DisplayName("OAuth2AuthenticationException(커스텀 에러 코드) 발생 시 해당 메시지 사용")
        void onAuthenticationFailure_withCustomErrorCode_usesCustomMessage() throws Exception {
            // given
            OAuth2Error error = new OAuth2Error(
                    AuthErrorCode.UNSUPPORTED_SOCIAL_TYPE.getCode(),
                    AuthErrorCode.UNSUPPORTED_SOCIAL_TYPE.getMessage(),
                    null
            );
            OAuth2AuthenticationException exception = new OAuth2AuthenticationException(error);

            // when
            handler.onAuthenticationFailure(request, response, exception);

            // then
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(redirectStrategy).sendRedirect(any(), any(), urlCaptor.capture());

            String redirectUrl = urlCaptor.getValue();
            String expectedEncodedMessage = URLEncoder.encode("지원하지 않는 소셜 로그인입니다", StandardCharsets.UTF_8);
            assertThat(redirectUrl).contains("error=" + expectedEncodedMessage);
        }

        @Test
        @DisplayName("OAuth2AuthenticationException(표준 에러 코드) 발생 시 기본 에러 메시지 사용")
        void onAuthenticationFailure_withStandardOAuth2Error_usesDefaultMessage() throws Exception {
            // given
            OAuth2Error error = new OAuth2Error("invalid_grant", "The authorization code has expired", null);
            OAuth2AuthenticationException exception = new OAuth2AuthenticationException(error);

            // when
            handler.onAuthenticationFailure(request, response, exception);

            // then
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(redirectStrategy).sendRedirect(any(), any(), urlCaptor.capture());

            String redirectUrl = urlCaptor.getValue();
            String expectedEncodedMessage = URLEncoder.encode("소셜 로그인에 실패했습니다", StandardCharsets.UTF_8);
            assertThat(redirectUrl).contains("error=" + expectedEncodedMessage);
            // OAuth2 표준 에러 상세 메시지가 노출되지 않음을 확인
            assertThat(redirectUrl).doesNotContain("authorization");
        }

        @Test
        @DisplayName("특수 문자가 포함된 에러 메시지도 URL 인코딩됨")
        void onAuthenticationFailure_withSpecialCharacters_encodesCorrectly() throws Exception {
            // given
            OAuth2Error error = new OAuth2Error(
                    AuthErrorCode.SOCIAL_LOGIN_FAILED.getCode(),
                    "로그인 실패: 잘못된 요청입니다",
                    null
            );
            OAuth2AuthenticationException exception = new OAuth2AuthenticationException(error);

            // when
            handler.onAuthenticationFailure(request, response, exception);

            // then
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(redirectStrategy).sendRedirect(any(), any(), urlCaptor.capture());

            String redirectUrl = urlCaptor.getValue();
            // URL에 한글이 인코딩되어 있음을 확인
            assertThat(redirectUrl).doesNotContain("로그인");
            assertThat(redirectUrl).contains("%");
        }
    }
}
