package com.gotcha.domain.auth.oauth2;

import com.gotcha.domain.auth.jwt.JwtTokenProvider;
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

    // TODO: 프로덕션 배포 전 리다이렉트 URI 화이트리스트 검증 추가 필요
    @Value("${oauth2.redirect-uri:http://localhost:3000/oauth/callback}")
    private String redirectUri;

    // TODO: HTTP-only 쿠키 방식으로 변경 예정
    // 현재 쿼리 파라미터 방식은 보안 취약점 존재:
    // - 브라우저 히스토리/서버 로그에 토큰 노출
    // - Referer 헤더를 통한 외부 유출 가능
    // 변경 시 프론트엔드 협의 필요 (credentials: 'include' 설정)
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();
        boolean isNewUser = oAuth2User.isNewUser();

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("isNewUser", isNewUser)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
