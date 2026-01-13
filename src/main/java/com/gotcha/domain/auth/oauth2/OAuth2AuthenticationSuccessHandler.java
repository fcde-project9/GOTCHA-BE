package com.gotcha.domain.auth.oauth2;

import com.gotcha.domain.auth.jwt.JwtTokenProvider;
import com.gotcha.domain.auth.service.AuthService;
import com.gotcha.domain.auth.service.OAuthTokenCacheService;
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
    private final OAuthTokenCacheService oAuthTokenCacheService;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieRepository;

    // TODO: 프로덕션 배포 전 리다이렉트 URI 화이트리스트 검증 추가 필요
    @Value("${oauth2.redirect-uri:http://localhost:3000/oauth/callback}")
    private String defaultRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();
        boolean isNewUser = oAuth2User.isNewUser();

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        // Refresh Token을 DB에 저장
        authService.saveRefreshToken(user, refreshToken);

        // 토큰을 캐시에 저장하고 임시 코드 발급 (보안 강화: URL에 토큰 노출 방지)
        String tempCode = oAuthTokenCacheService.storeTokens(accessToken, refreshToken, isNewUser);

        // 프론트엔드에서 전달한 redirect_uri 사용, 없으면 기본값 사용
        String redirectUri = cookieRepository.getRedirectUriFromCookie(request);
        if (redirectUri == null || redirectUri.isBlank()) {
            redirectUri = defaultRedirectUri;
        }
        cookieRepository.removeRedirectUriCookie(response);

        // 임시 코드만 전달 (토큰은 POST /api/auth/token으로 교환, isNewUser는 응답에 포함)
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", tempCode)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
