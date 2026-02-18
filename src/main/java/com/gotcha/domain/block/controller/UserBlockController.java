package com.gotcha.domain.block.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha._global.common.PageResponse;
import com.gotcha.domain.block.dto.BlockResponse;
import com.gotcha.domain.block.dto.BlockedUserResponse;
import com.gotcha.domain.block.service.UserBlockService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserBlockController implements UserBlockControllerApi {

    private final UserBlockService userBlockService;

    @Override
    @PostMapping("/{userId}/block")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BlockResponse> blockUser(@PathVariable Long userId) {
        return ApiResponse.success(userBlockService.blockUser(userId));
    }

    @Override
    @DeleteMapping("/{userId}/block")
    public ApiResponse<Void> unblockUser(@PathVariable Long userId) {
        userBlockService.unblockUser(userId);
        return ApiResponse.success(null);
    }

    @Override
    @GetMapping("/me/blocks")
    public ApiResponse<PageResponse<BlockedUserResponse>> getMyBlocks(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(userBlockService.getMyBlocks(pageable));
    }
}
