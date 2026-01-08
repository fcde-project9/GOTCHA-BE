package com.gotcha.domain.user.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.user.dto.PermissionResponse;
import com.gotcha.domain.user.dto.UpdatePermissionRequest;
import com.gotcha.domain.user.entity.PermissionType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.service.UserPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User Permission", description = "사용자 권한 동의 API")
@RestController
@RequestMapping("/api/users/permissions")
@RequiredArgsConstructor
public class UserPermissionController {

    private final UserPermissionService userPermissionService;
    private final SecurityUtil securityUtil;

    @Operation(
            summary = "권한 동의 여부 확인",
            description = "특정 권한(LOCATION, CAMERA, ALBUM)의 동의 여부를 확인합니다",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{permissionType}")
    public ApiResponse<PermissionResponse> checkPermission(@PathVariable PermissionType permissionType) {
        Long userId = securityUtil.getCurrentUserId();
        boolean isGranted = userPermissionService.isPermissionGranted(userId, permissionType);
        return ApiResponse.success(PermissionResponse.of(permissionType, isGranted));
    }

    @Operation(
            summary = "권한 동의 상태 업데이트",
            description = "사용자의 권한 동의 상태를 업데이트합니다. 변경 이력이 자동으로 저장됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ApiResponse<PermissionResponse> updatePermission(
            @Valid @RequestBody UpdatePermissionRequest request,
            HttpServletRequest httpRequest
    ) {
        User currentUser = securityUtil.getCurrentUser();

        // User-Agent에서 디바이스 정보 추출
        String deviceInfo = httpRequest.getHeader("User-Agent");

        userPermissionService.updatePermission(
                currentUser,
                request.permissionType(),
                request.isAgreed(),
                deviceInfo
        );

        return ApiResponse.success(
                PermissionResponse.of(request.permissionType(), request.isAgreed())
        );
    }
}
