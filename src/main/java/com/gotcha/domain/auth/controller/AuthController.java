package com.gotcha.domain.auth.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.auth.dto.ReissueRequest;
import com.gotcha.domain.auth.dto.TokenExchangeRequest;
import com.gotcha.domain.auth.dto.TokenExchangeResponse;
import com.gotcha.domain.auth.dto.TokenResponse;
import com.gotcha.domain.auth.service.AuthService;
import com.gotcha.domain.auth.service.OAuthTokenCookieService;
import com.gotcha.domain.auth.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerApi {

    private final AuthService authService;
    private final SecurityUtil securityUtil;
    private final OAuthTokenCookieService oAuthTokenCacheService;
    private final Environment environment;

    @Override
    @PostMapping("/reissue")
    public ApiResponse<TokenResponse> reissue(@Valid @RequestBody ReissueRequest request) {
        TokenResponse response = authService.reissueToken(request.refreshToken());
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        Long userId = securityUtil.getCurrentUserId();
        authService.logout(userId);

        // 인증 관련 쿠키 삭제 (HttpOnly 쿠키는 Set-Cookie 헤더로만 삭제 가능)
        CookieUtils.clearAuthCookies(request, response);

        return ApiResponse.success(null);
    }

    @Override
    @PostMapping("/token")
    public ApiResponse<TokenExchangeResponse> exchangeToken(
            @Valid @RequestBody TokenExchangeRequest request) {
        // body의 code를 복호화하여 토큰 반환 (쿠키 불필요 - cross-site 지원)
        TokenExchangeResponse tokenResponse = authService.exchangeToken(request.code());
        return ApiResponse.success(tokenResponse);
    }

    @Override
    @PostMapping("/test-code")
    public ApiResponse<String> generateTestCode() {
        // local, dev 환경에서만 동작
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isAllowedProfile = false;
        for (String profile : activeProfiles) {
            if ("local".equals(profile) || "dev".equals(profile)) {
                isAllowedProfile = true;
                break;
            }
        }

        if (!isAllowedProfile) {
            throw new IllegalStateException("This endpoint is only available in local/dev environment");
        }

        String encryptedToken = oAuthTokenCacheService.storeTokensForTest(
                "test-access-token-for-swagger",
                "test-refresh-token-for-swagger",
                false
        );
        return ApiResponse.success(encryptedToken);
    }
}
