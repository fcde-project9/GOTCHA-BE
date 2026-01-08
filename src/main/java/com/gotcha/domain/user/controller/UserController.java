package com.gotcha.domain.user.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha._global.common.PageResponse;
import com.gotcha.domain.favorite.dto.FavoriteShopResponse;
import com.gotcha.domain.favorite.service.FavoriteService;
import com.gotcha.domain.user.dto.UpdateNicknameRequest;
import com.gotcha.domain.user.dto.UserResponse;
import com.gotcha.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FavoriteService favoriteService;

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
                                        "profileImageUrl": null,
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

    @Operation(
            summary = "내 찜 목록 조회",
            description = "현재 로그인한 사용자의 찜 목록을 조회합니다. lat, lng 파라미터를 제공하면 거리가 계산됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/me/favorites")
    public ApiResponse<PageResponse<FavoriteShopResponse>> getMyFavorites(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(favoriteService.getMyFavorites(lat, lng, pageable));
    }

    @Operation(
            summary = "닉네임 변경",
            description = "현재 로그인한 사용자의 닉네임을 변경합니다. 중복된 닉네임은 사용할 수 없습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "변경 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 1,
                                        "nickname": "새닉네임#99",
                                        "email": "user@example.com",
                                        "profileImageUrl": null,
                                        "socialType": "KAKAO"
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
                                    name = "U002 - 닉네임 형식 오류",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "U002",
                                                "message": "닉네임 형식이 올바르지 않습니다"
                                              }
                                            }
                                            """
                            )
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "닉네임 중복",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "U001 - 닉네임 중복",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "U001",
                                                "message": "이미 사용 중인 닉네임입니다"
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    @PatchMapping("/me/nickname")
    public ApiResponse<UserResponse> updateNickname(
            @Valid @RequestBody UpdateNicknameRequest request
    ) {
        return ApiResponse.success(userService.updateNickname(request.nickname()));
    }
}
