package com.gotcha.domain.user.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha._global.common.PageResponse;
import com.gotcha.domain.favorite.dto.FavoriteShopResponse;
import com.gotcha.domain.user.dto.MyShopResponse;
import com.gotcha.domain.user.dto.UpdateNicknameRequest;
import com.gotcha.domain.user.dto.UpdateProfileImageRequest;
import com.gotcha.domain.user.dto.UserNicknameResponse;
import com.gotcha.domain.user.dto.UserResponse;
import com.gotcha.domain.user.dto.WithdrawalRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "User", description = "사용자 API")
public interface UserControllerApi {

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
                                        "profileImageUrl": "https://storage.googleapis.com/gotcha-dev-files/profile-default-join.png",
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
    ApiResponse<UserResponse> getMyInfo();

    @Operation(
            summary = "내 찜 목록 조회",
            description = "현재 로그인한 사용자의 전체 찜 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
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
    ApiResponse<List<FavoriteShopResponse>> getMyFavorites();

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
    ApiResponse<PageResponse<MyShopResponse>> getMyShops(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    );

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
                                        "profileImageUrl": "https://storage.googleapis.com/gotcha-dev-files/profile-default-join.png",
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
    ApiResponse<UserResponse> updateNickname(@Valid @RequestBody UpdateNicknameRequest request);

    @Operation(
            summary = "내 닉네임 조회",
            description = "현재 로그인한 사용자의 닉네임만 조회합니다",
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
                                        "nickname": "빨간캡슐#21"
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
                                            name = "로그인 필요",
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
                                            name = "탈퇴한 사용자",
                                            value = """
                                                    {
                                                      "success": false,
                                                      "error": {
                                                        "code": "A012",
                                                        "message": "탈퇴한 사용자입니다"
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    ApiResponse<UserNicknameResponse> getNickname();

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
    ApiResponse<UserResponse> updateProfileImage(@Valid @RequestBody UpdateProfileImageRequest request);

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
                                        "profileImageUrl": "https://storage.googleapis.com/gotcha-dev-files/profile-default-join.png",
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
    ApiResponse<UserResponse> deleteProfileImage();

    @Operation(
            summary = "회원 탈퇴",
            description = """
                    회원 탈퇴 및 설문 제출.

                    **처리 내역:**
                    - Refresh Token 삭제
                    - 인증 관련 쿠키 삭제 (Set-Cookie 헤더로 응답)
                    - 사용자 soft delete

                    **주의:** 탈퇴 후 프론트엔드에서 localStorage의 토큰도 삭제해야 합니다.
                    """,
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
    ApiResponse<Void> withdraw(
            @Valid @RequestBody WithdrawalRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    );
}
