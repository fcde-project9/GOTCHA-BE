package com.gotcha.domain.auth.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.auth.dto.ReissueRequest;
import com.gotcha.domain.auth.dto.TokenExchangeRequest;
import com.gotcha.domain.auth.dto.TokenExchangeResponse;
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

    @Operation(
            summary = "임시 코드로 토큰 교환",
            description = """
                    OAuth 로그인 완료 후 발급된 임시 코드를 실제 토큰으로 교환합니다.

                    **플로우:**
                    1. 소셜 로그인 완료 → 프론트엔드로 임시 코드(code) 전달
                    2. 프론트엔드가 이 API를 호출하여 실제 토큰 획득

                    **보안:**
                    - 임시 코드는 30초 후 만료됩니다
                    - 1회 사용 후 즉시 무효화됩니다
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "토큰 교환 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "accessToken": "ACCESS_TOKEN_EXAMPLE",
                                        "refreshToken": "REFRESH_TOKEN_EXAMPLE",
                                        "isNewUser": false
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "A013 - 유효하지 않은 코드",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "A013",
                                                "message": "유효하지 않거나 만료된 인증 코드입니다"
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    ApiResponse<TokenExchangeResponse> exchangeToken(@Valid @RequestBody TokenExchangeRequest request);

    @Operation(
            summary = "[테스트용] 임시 코드 생성",
            description = """
                    **local, dev 환경에서만 동작합니다.**

                    Swagger에서 토큰 교환 API를 테스트하기 위한 임시 코드를 생성합니다.

                    **사용 방법:**
                    1. 이 API 호출 → 임시 코드 발급
                    2. 30초 내에 POST /api/auth/token 호출
                    3. 발급받은 코드로 토큰 교환 테스트

                    **주의:** 운영 환경에서는 500 에러가 발생합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "테스트용 임시 코드 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": "550e8400-e29b-41d4-a716-446655440000"
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "운영 환경에서 호출 시",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "G001",
                                        "message": "서버 내부 오류가 발생했습니다"
                                      }
                                    }
                                    """)
                    )
            )
    })
    ApiResponse<String> generateTestCode();
}
