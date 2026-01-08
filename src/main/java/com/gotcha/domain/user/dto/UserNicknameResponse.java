package com.gotcha.domain.user.dto;

import com.gotcha.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 닉네임 응답")
public record UserNicknameResponse(
        @Schema(description = "닉네임", example = "빨간캡슐#21")
        String nickname
) {
    public static UserNicknameResponse from(User user) {
        return new UserNicknameResponse(user.getNickname());
    }
}
