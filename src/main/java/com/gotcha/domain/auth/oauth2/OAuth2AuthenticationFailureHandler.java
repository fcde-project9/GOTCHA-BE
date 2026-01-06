package com.gotcha.domain.auth.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final String DEFAULT_ERROR_MESSAGE = "소셜 로그인에 실패했습니다";

    @Value("${oauth2.redirect-uri:http://localhost:3000/oauth/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.error("OAuth2 authentication failed: {}", exception.getMessage(), exception);

        // 사용자에게 보여줄 안전한 에러 메시지 결정
        String errorMessage = getSafeErrorMessage(exception);

        // URL 인코딩된 에러 메시지로 리다이렉트
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", URLEncoder.encode(errorMessage, StandardCharsets.UTF_8))
                .build(true)  // encoded=true로 이미 인코딩된 값 유지
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String getSafeErrorMessage(AuthenticationException exception) {
        // OAuth2AuthenticationException의 경우 커스텀 에러 코드 확인
        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            String errorCode = oauth2Exception.getError().getErrorCode();
            // 커스텀 에러 코드인 경우 해당 메시지 사용
            if (errorCode != null && errorCode.startsWith("A")) {
                String description = oauth2Exception.getError().getDescription();
                return description != null ? description : DEFAULT_ERROR_MESSAGE;
            }
        }
        // 기본 에러 메시지 (내부 에러 노출 방지)
        return DEFAULT_ERROR_MESSAGE;
    }
}
