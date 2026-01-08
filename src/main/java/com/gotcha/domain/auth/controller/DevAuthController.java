package com.gotcha.domain.auth.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.auth.jwt.JwtTokenProvider;
import com.gotcha.domain.auth.service.AuthService;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dev", description = "개발용 API (local/dev 환경에서만 동작)")
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
@Profile({"local", "dev"})
public class DevAuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final AuthService authService;

    @Operation(
            summary = "테스트용 토큰 발급",
            description = "userId로 테스트용 JWT 토큰을 발급합니다. local/dev 환경에서만 동작합니다."
    )
    @GetMapping("/token")
    public ApiResponse<DevTokenResponse> getTestToken(@RequestParam Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        // Refresh Token을 DB에 저장
        authService.saveRefreshToken(user, refreshToken);

        return ApiResponse.success(new DevTokenResponse(accessToken, refreshToken));
    }

    public record DevTokenResponse(String accessToken, String refreshToken) {}
}
