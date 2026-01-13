package com.gotcha.domain.auth.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.auth.dto.ReissueRequest;
import com.gotcha.domain.auth.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth", description = "인증 API")
public interface AuthControllerApi {

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
                                        "accessToken": "ACCESS_TOKEN_EXAMPLE",
                                        "refreshToken": "REFRESH_TOKEN_EXAMPLE",
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
    ApiResponse<TokenResponse> reissue(@Valid @RequestBody ReissueRequest request);

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
    ApiResponse<Void> logout();
}
