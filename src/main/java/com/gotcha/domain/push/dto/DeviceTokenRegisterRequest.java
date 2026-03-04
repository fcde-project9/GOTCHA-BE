package com.gotcha.domain.push.dto;

import com.gotcha.domain.push.entity.DevicePlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "네이티브 푸시 기기 등록 요청")
public record DeviceTokenRegisterRequest(

    @Schema(description = "APNS/FCM 디바이스 토큰", example = "a1b2c3d4e5f6...")
    @NotBlank(message = "deviceToken은 필수입니다")
    @Size(max = 200, message = "deviceToken은 최대 200자입니다")
    String deviceToken,

    @Schema(description = "플랫폼", example = "IOS")
    @NotNull(message = "platform은 필수입니다")
    DevicePlatform platform
) {}
