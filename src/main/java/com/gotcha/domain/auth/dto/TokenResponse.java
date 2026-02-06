package com.gotcha.domain.auth.dto;

import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.entity.UserType;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {
    public static TokenResponse of(String accessToken, String refreshToken, User user, boolean isNewUser) {
        return new TokenResponse(accessToken, refreshToken, UserResponse.from(user, isNewUser));
    }

    public record UserResponse(
            Long id,
            String nickname,
            String email,
            SocialType socialType,
            UserType userType,
            boolean isNewUser
    ) {
        public static UserResponse from(User user, boolean isNewUser) {
            return new UserResponse(
                    user.getId(),
                    user.getNickname(),
                    user.getEmail(),
                    user.getSocialType(),
                    user.getUserType(),
                    isNewUser
            );
        }
    }
}
