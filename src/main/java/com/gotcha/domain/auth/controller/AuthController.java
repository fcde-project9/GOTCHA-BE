package com.gotcha.domain.auth.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.auth.dto.ReissueRequest;
import com.gotcha.domain.auth.dto.TokenExchangeRequest;
import com.gotcha.domain.auth.dto.TokenExchangeResponse;
import com.gotcha.domain.auth.dto.TokenResponse;
import com.gotcha.domain.auth.service.AuthService;
import com.gotcha.domain.auth.service.OAuthTokenCacheService;
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
    private final OAuthTokenCacheService oAuthTokenCacheService;
    private final Environment environment;

    @Override
    @PostMapping("/reissue")
    public ApiResponse<TokenResponse> reissue(@Valid @RequestBody ReissueRequest request) {
        TokenResponse response = authService.reissueToken(request.refreshToken());
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        Long userId = securityUtil.getCurrentUserId();
        authService.logout(userId);
        return ApiResponse.success(null);
    }

    @Override
    @PostMapping("/token")
    public ApiResponse<TokenExchangeResponse> exchangeToken(
            @Valid @RequestBody TokenExchangeRequest request) {
        TokenExchangeResponse response = authService.exchangeToken(request.code());
        return ApiResponse.success(response);
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

        String code = oAuthTokenCacheService.storeTokens(
                "test-access-token-for-swagger",
                "test-refresh-token-for-swagger",
                false
        );
        return ApiResponse.success(code);
    }
}
