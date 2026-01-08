package com.gotcha.domain.auth.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.auth.dto.ReissueRequest;
import com.gotcha.domain.auth.dto.TokenResponse;
import com.gotcha.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    private final SecurityUtil securityUtil;

    @Operation(
            summary = "토큰 재발급",
            description = "리프레시 토큰으로 새로운 액세스 토큰과 리프레시 토큰을 발급받습니다"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "재발급 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                        "user": {
                                          "id": 1,
                                          "nickname": "빨간캡슐#21",
                                          "email": "user@example.com",
                                          "socialType": "KAKAO",
                                          "isNewUser": false
                                        }
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "A010 - 리프레시 토큰 없음",
                                            value = """
                                                    {
                                                      "success": false,
                                                      "error": {
                                                        "code": "A010",
                                                        "message": "리프레시 토큰을 찾을 수 없습니다"
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "A011 - 리프레시 토큰 만료",
                                            value = """
                                                    {
                                                      "success": false,
                                                      "error": {
                                                        "code": "A011",
                                                        "message": "리프레시 토큰이 만료되었습니다"
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    @PostMapping("/reissue")
    public ApiResponse<TokenResponse> reissue(@Valid @RequestBody ReissueRequest request) {
        TokenResponse response = authService.reissueToken(request.refreshToken());
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "로그아웃",
            description = "리프레시 토큰을 무효화하여 로그아웃합니다",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "A001 - 토큰 없음",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "A001",
                                                "message": "로그인이 필요합니다"
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        Long userId = securityUtil.getCurrentUserId();
        authService.logout(userId);
        return ApiResponse.success(null);
    }
}
