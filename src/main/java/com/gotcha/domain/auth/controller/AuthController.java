package com.gotcha.domain.auth.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.auth.dto.ReissueRequest;
import com.gotcha.domain.auth.dto.TokenResponse;
import com.gotcha.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
