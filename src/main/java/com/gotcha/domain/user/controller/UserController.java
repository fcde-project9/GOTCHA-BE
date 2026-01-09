package com.gotcha.domain.user.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha._global.common.PageResponse;
import com.gotcha.domain.favorite.dto.FavoriteShopResponse;
import com.gotcha.domain.favorite.service.FavoriteService;
import com.gotcha.domain.user.dto.MyShopResponse;
import com.gotcha.domain.user.dto.UpdateNicknameRequest;
import com.gotcha.domain.user.dto.UpdateProfileImageRequest;
import com.gotcha.domain.user.dto.UserNicknameResponse;
import com.gotcha.domain.user.dto.UserResponse;
import com.gotcha.domain.user.dto.WithdrawalRequest;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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
            summary = "내가 제보한 가게 목록 조회",
            description = "현재 로그인한 사용자가 제보한 가게 목록을 최신순으로 조회합니다.",
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
                                        "content": [
                                          {
                                            "id": 1,
                                            "name": "가챠샵 신사점",
                                            "mainImageUrl": "https://...",
                                            "addressName": "서울시 강남구 신사동 123-45",
                                            "isOpen": true,
                                            "createdAt": "2025-01-01"
                                          }
                                        ],
                                        "totalCount": 2,
                                        "page": 0,
                                        "size": 20,
                                        "hasNext": false
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
    @GetMapping("/me/shops")
    public ApiResponse<PageResponse<MyShopResponse>> getMyShops(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(userService.getMyShops(pageable));
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

    @Operation(
            summary = "내 닉네임 조회",
            description = "현재 로그인한 사용자의 닉네임만 조회합니다",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/me/nickname")
    public ApiResponse<UserNicknameResponse> getNickname() {
        return ApiResponse.success(userService.getNickname());
    }

    @Operation(
            summary = "프로필 이미지 변경",
            description = "현재 로그인한 사용자의 프로필 이미지를 변경합니다. 먼저 /api/files/upload로 이미지를 업로드한 후, 반환된 URL을 사용하세요.",
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
                                        "nickname": "빨간캡슐#21",
                                        "email": "user@example.com",
                                        "profileImageUrl": "https://storage.googleapis.com/gotcha-dev-files/profiles/abc-123.webp",
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
                                    name = "C001 - URL 형식 오류",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "C001",
                                                "message": "올바른 GCS URL 형식이 아닙니다"
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
            )
    })
    @PatchMapping("/me/profile-image")
    public ApiResponse<UserResponse> updateProfileImage(
            @Valid @RequestBody UpdateProfileImageRequest request
    ) {
        return ApiResponse.success(userService.updateProfileImage(request.profileImageUrl()));
    }

    @Operation(
            summary = "프로필 이미지 삭제",
            description = "현재 로그인한 사용자의 프로필 이미지를 삭제하고 기본 프로필 이미지로 복구합니다. 커스텀 이미지는 GCS에서 자동 삭제됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공 (기본 이미지로 복구)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 1,
                                        "nickname": "빨간캡슐#21",
                                        "email": "user@example.com",
                                        "profileImageUrl": "https://storage.googleapis.com/gotcha-dev-files/defaults/profile-default-join.png",
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
    @DeleteMapping("/me/profile-image")
    public ApiResponse<UserResponse> deleteProfileImage() {
        return ApiResponse.success(userService.deleteProfileImage());
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "회원 탈퇴 및 설문 제출. 탈퇴 시 Refresh Token이 삭제되고 사용자는 soft delete됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "탈퇴 성공",
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
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "C001 - 필수 파라미터 누락",
                                            value = """
                                                    {
                                                      "success": false,
                                                      "error": {
                                                        "code": "C001",
                                                        "message": "탈퇴 사유는 최소 1개 이상 선택해야 합니다"
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "U005 - 이미 탈퇴한 사용자",
                                            value = """
                                                    {
                                                      "success": false,
                                                      "error": {
                                                        "code": "U005",
                                                        "message": "이미 탈퇴한 사용자입니다"
                                                      }
                                                    }
                                                    """
                                    )
                            }
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
    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(@Valid @RequestBody WithdrawalRequest request) {
        userService.withdraw(request);
        return ApiResponse.success(null);
    }
}
