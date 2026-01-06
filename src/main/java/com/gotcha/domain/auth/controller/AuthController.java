package com.gotcha.domain.auth.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.auth.dto.LoginRequest;
import com.gotcha.domain.auth.dto.TokenResponse;
import com.gotcha.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "소셜 로그인",
            description = "소셜 액세스 토큰을 받아 JWT 토큰을 발급합니다"
    )
    @PostMapping("/login/{provider}")
    public ApiResponse<TokenResponse> login(
            @PathVariable String provider,
            @RequestBody @Valid LoginRequest request
    ) {
        TokenResponse token = authService.login(provider, request.accessToken());
        return ApiResponse.success(token);
    }
}
