package com.gotcha.domain.auth.oauth2;

import com.gotcha.domain.auth.jwt.JwtTokenProvider;
import com.gotcha.domain.auth.service.AuthService;
import com.gotcha.domain.auth.service.OAuthTokenCookieService;
import com.gotcha.domain.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;
    private final OAuthTokenCookieService oAuthTokenCacheService;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieRepository;

    // TODO: 프로덕션 배포 전 리다이렉트 URI 화이트리스트 검증 추가 필요
    @Value("${oauth2.redirect-uri:http://localhost:3000/oauth/callback}")
    private String defaultRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        User user;
        boolean isNewUser;

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomOidcUser oidcUser) {
            // Apple (OIDC) 로그인
            user = oidcUser.getUser();
            isNewUser = oidcUser.isNewUser();
        } else if (principal instanceof CustomOAuth2User oAuth2User) {
            // 카카오/구글/네이버 (OAuth2) 로그인
            user = oAuth2User.getUser();
            isNewUser = oAuth2User.isNewUser();
        } else {
            throw new IllegalStateException("Unexpected principal type: " + principal.getClass());
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        // Refresh Token을 DB에 저장
        authService.saveRefreshToken(user, refreshToken);

        // 토큰을 암호화하여 URL 파라미터로 전달 (쿠키 미사용 - cross-site 지원)
        String encryptedCode = oAuthTokenCacheService.encryptTokens(accessToken, refreshToken, isNewUser);

        // 프론트엔드에서 전달한 redirect_uri 사용, 없으면 기본값 사용
        String redirectUri = cookieRepository.getRedirectUriFromCookie(request);
        if (redirectUri == null || redirectUri.isBlank()) {
            redirectUri = defaultRedirectUri;
        }
        cookieRepository.removeRedirectUriCookie(response);

        // 암호화된 토큰을 URL 파라미터로 전달 (프론트엔드가 POST /api/auth/token body에 포함)
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", encryptedCode)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
