package com.gotcha.domain.push.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "VAPID 공개키 응답")
public record VapidKeyResponse(

    @Schema(description = "VAPID 공개키", example = "BEl62iUYgUivxIkv69yViEuiBIa-Ib9...")
    String publicKey
) {
    public static VapidKeyResponse of(String publicKey) {
        return new VapidKeyResponse(publicKey);
    }
}
