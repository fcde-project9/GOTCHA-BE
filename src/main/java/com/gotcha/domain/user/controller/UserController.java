package com.gotcha.domain.user.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha._global.common.PageResponse;
import com.gotcha.domain.auth.util.CookieUtils;
import com.gotcha.domain.favorite.dto.FavoriteShopResponse;
import com.gotcha.domain.favorite.service.FavoriteService;
import com.gotcha.domain.user.dto.MyShopResponse;
import com.gotcha.domain.user.dto.UpdateNicknameRequest;
import com.gotcha.domain.user.dto.UpdateProfileImageRequest;
import com.gotcha.domain.user.dto.UserNicknameResponse;
import com.gotcha.domain.user.dto.UserResponse;
import com.gotcha.domain.user.dto.WithdrawalRequest;
import com.gotcha.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController implements UserControllerApi {

    private final UserService userService;
    private final FavoriteService favoriteService;

    @Override
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.success(userService.getMyInfo());
    }

    @Override
    @GetMapping("/me/favorites")
    public ApiResponse<List<FavoriteShopResponse>> getMyFavorites() {
        return ApiResponse.success(favoriteService.getMyFavorites());
    }

    @Override
    @GetMapping("/me/shops")
    public ApiResponse<PageResponse<MyShopResponse>> getMyShops(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(userService.getMyShops(pageable));
    }

    @Override
    @PatchMapping("/me/nickname")
    public ApiResponse<UserResponse> updateNickname(@Valid @RequestBody UpdateNicknameRequest request) {
        return ApiResponse.success(userService.updateNickname(request.nickname()));
    }

    @Override
    @GetMapping("/me/nickname")
    public ApiResponse<UserNicknameResponse> getNickname() {
        return ApiResponse.success(userService.getNickname());
    }

    @Override
    @PatchMapping("/me/profile-image")
    public ApiResponse<UserResponse> updateProfileImage(@Valid @RequestBody UpdateProfileImageRequest request) {
        return ApiResponse.success(userService.updateProfileImage(request.profileImageUrl()));
    }

    @Override
    @DeleteMapping("/me/profile-image")
    public ApiResponse<UserResponse> deleteProfileImage() {
        return ApiResponse.success(userService.deleteProfileImage());
    }

    @Override
    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(
            @Valid @RequestBody WithdrawalRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        userService.withdraw(request);

        // 인증 관련 쿠키 삭제 (HttpOnly 쿠키는 Set-Cookie 헤더로만 삭제 가능)
        CookieUtils.clearAuthCookies(httpRequest, httpResponse);

        return ApiResponse.success(null);
    }
}
