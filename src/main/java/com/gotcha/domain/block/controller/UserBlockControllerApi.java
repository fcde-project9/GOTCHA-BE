package com.gotcha.domain.block.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha._global.common.PageResponse;
import com.gotcha.domain.block.dto.BlockResponse;
import com.gotcha.domain.block.dto.BlockedUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "User Block", description = "사용자 차단 API")
public interface UserBlockControllerApi {

    @Operation(
            summary = "사용자 차단",
            description = "지정된 사용자를 차단합니다. 차단된 사용자의 리뷰는 목록에서 보이지 않습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "차단 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "blockedUserId": 123,
                                        "blockedAt": "2025-01-08T12:00:00"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "본인 차단 시도",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "BK001 - 본인 차단 불가",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "BK001",
                                                "message": "본인을 차단할 수 없습니다"
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "U004 - 사용자 없음",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "U004",
                                                "message": "사용자를 찾을 수 없습니다"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 차단됨",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "BK002 - 이미 차단",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "BK002",
                                                "message": "이미 차단한 사용자입니다"
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    ApiResponse<BlockResponse> blockUser(@PathVariable Long userId);

    @Operation(
            summary = "사용자 차단 해제",
            description = "지정된 사용자의 차단을 해제합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "차단 해제 성공",
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "차단 정보 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "BK003 - 차단 정보 없음",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "BK003",
                                                "message": "차단 정보를 찾을 수 없습니다"
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    ApiResponse<Void> unblockUser(@PathVariable Long userId);

    @Operation(
            summary = "내 차단 목록 조회",
            description = "현재 로그인한 사용자가 차단한 사용자 목록을 조회합니다.",
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
                                            "id": 123,
                                            "nickname": "차단된유저#21",
                                            "profileImageUrl": "https://...",
                                            "blockedAt": "2025-01-08T12:00:00"
                                          }
                                        ],
                                        "totalCount": 5,
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
    ApiResponse<PageResponse<BlockedUserResponse>> getMyBlocks(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    );
}
