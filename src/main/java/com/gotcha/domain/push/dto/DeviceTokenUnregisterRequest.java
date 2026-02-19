package com.gotcha.domain.push.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "네이티브 푸시 기기 해제 요청")
public record DeviceTokenUnregisterRequest(

    @Schema(description = "APNS/FCM 디바이스 토큰", example = "a1b2c3d4e5f6...")
    @NotBlank(message = "deviceToken은 필수입니다")
    String deviceToken
) {}
