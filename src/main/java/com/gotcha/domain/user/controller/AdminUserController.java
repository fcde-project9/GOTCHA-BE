package com.gotcha.domain.user.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.user.dto.AdminUserListResponse;
import com.gotcha.domain.user.dto.AdminUserResponse;
import com.gotcha.domain.user.dto.UpdateUserStatusRequest;
import com.gotcha.domain.user.entity.UserStatus;
import com.gotcha.domain.user.service.AdminUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Validated
public class AdminUserController implements AdminUserControllerApi {

    private final AdminUserService adminUserService;

    @Override
    @GetMapping
    public ApiResponse<AdminUserListResponse> getUsers(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        AdminUserListResponse response = adminUserService.getUsers(status, pageable);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/{userId}")
    public ApiResponse<AdminUserResponse> getUser(@PathVariable Long userId) {
        AdminUserResponse response = adminUserService.getUser(userId);
        return ApiResponse.success(response);
    }

    @Override
    @PatchMapping("/{userId}/status")
    public ApiResponse<AdminUserResponse> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        AdminUserResponse response = adminUserService.updateUserStatus(userId, request);
        return ApiResponse.success(response);
    }
}
