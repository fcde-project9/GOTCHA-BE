package com.gotcha.domain.auth.oauth2;

import com.gotcha.domain.auth.exception.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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

    private final InMemoryAuthorizationRequestRepository authorizationRequestRepository;

    @Value("${oauth2.redirect-uri:http://localhost:3000/oauth/callback}")
    private String defaultRedirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.error("OAuth2 authentication failed: {}", exception.getMessage(), exception);

        AuthErrorCode errorCode = resolveErrorCode(exception);

        // 프론트엔드에서 전달한 redirect_uri 사용, 없으면 기본값 사용
        String redirectUri = authorizationRequestRepository.getRedirectUri(request);
        if (redirectUri == null || redirectUri.isBlank()) {
            redirectUri = defaultRedirectUri;
        }

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", errorCode.getCode())
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private AuthErrorCode resolveErrorCode(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            String errorCode = oauth2Exception.getError().getErrorCode();
            return switch (errorCode) {
                case "access_denied" -> AuthErrorCode.OAUTH_ACCESS_DENIED;
                case "invalid_token" -> AuthErrorCode.OAUTH_INVALID_TOKEN;
                case "invalid_response" -> AuthErrorCode.OAUTH_INVALID_RESPONSE;
                default -> AuthErrorCode.SOCIAL_LOGIN_FAILED;
            };
        }
        return AuthErrorCode.SOCIAL_LOGIN_FAILED;
    }
}
