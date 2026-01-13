package com.gotcha.domain.user.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.user.dto.PermissionResponse;
import com.gotcha.domain.user.dto.UpdatePermissionRequest;
import com.gotcha.domain.user.entity.PermissionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User Permission", description = "사용자 권한 동의 API")
public interface UserPermissionControllerApi {

    @Operation(
            summary = "권한 동의 여부 확인",
            description = "특정 권한(LOCATION, CAMERA, ALBUM)의 동의 여부를 확인합니다",
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
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "A001",
                                        "message": "로그인이 필요합니다"
                                      }
                                    }
                                    """)
                    )
            )
    })
    ApiResponse<PermissionResponse> checkPermission(@PathVariable PermissionType permissionType);

    @Operation(
            summary = "권한 동의 상태 업데이트",
            description = "사용자의 권한 동의 상태를 업데이트합니다. 변경 이력이 자동으로 저장됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "업데이트 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "INVALID_INPUT",
                                        "message": "입력값이 올바르지 않습니다"
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
    ApiResponse<PermissionResponse> updatePermission(
            @Valid @RequestBody UpdatePermissionRequest request,
            HttpServletRequest httpRequest
    );
}
