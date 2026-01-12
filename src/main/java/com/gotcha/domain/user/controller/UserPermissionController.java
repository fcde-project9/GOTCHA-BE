package com.gotcha.domain.user.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.user.dto.PermissionResponse;
import com.gotcha.domain.user.dto.UpdatePermissionRequest;
import com.gotcha.domain.user.entity.PermissionType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.service.UserPermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/permissions")
@RequiredArgsConstructor
public class UserPermissionController implements UserPermissionControllerApi {

    private final UserPermissionService userPermissionService;
    private final SecurityUtil securityUtil;

    @Override
    @GetMapping("/{permissionType}")
    public ApiResponse<PermissionResponse> checkPermission(@PathVariable PermissionType permissionType) {
        Long userId = securityUtil.getCurrentUserId();
        boolean isGranted = userPermissionService.isPermissionGranted(userId, permissionType);
        return ApiResponse.success(PermissionResponse.of(permissionType, isGranted));
    }

    @Override
    @PostMapping
    public ApiResponse<PermissionResponse> updatePermission(
            @Valid @RequestBody UpdatePermissionRequest request,
            HttpServletRequest httpRequest
    ) {
        User currentUser = securityUtil.getCurrentUser();
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
