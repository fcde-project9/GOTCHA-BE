package com.gotcha.domain.user.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.user.dto.UserResponse;
import com.gotcha.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "내 정보 조회",
            description = "현재 로그인한 사용자의 정보를 조회합니다",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 1,
                                        "nickname": "빨간캡슐#21",
                                        "email": "user@example.com",
                                        "profileImageUrl": "https://k.kakaocdn.net/...",
                                        "socialType": "KAKAO"
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
                                    ),
                                    @ExampleObject(
                                            name = "A003 - 토큰 만료",
                                            value = """
                                                    {
                                                      "success": false,
                                                      "error": {
                                                        "code": "A003",
                                                        "message": "토큰이 만료되었습니다"
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "A004 - 유효하지 않은 토큰",
                                            value = """
                                                    {
                                                      "success": false,
                                                      "error": {
                                                        "code": "A004",
                                                        "message": "유효하지 않은 토큰입니다"
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.success(userService.getMyInfo());
    }
}
