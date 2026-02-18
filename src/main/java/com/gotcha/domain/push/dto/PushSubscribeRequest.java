package com.gotcha.domain.push.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "푸시 알림 구독 요청")
public record PushSubscribeRequest(

    @Schema(description = "브라우저 푸시 엔드포인트 URL", example = "https://fcm.googleapis.com/fcm/send/...")
    @NotBlank(message = "endpoint는 필수입니다")
    String endpoint,

    @Schema(description = "암호화 키")
    @NotNull(message = "keys는 필수입니다")
    @Valid
    Keys keys
) {
    @Schema(description = "푸시 알림 암호화 키")
    public record Keys(
        @Schema(description = "P-256 Diffie-Hellman 공개키", example = "BNcRdreALRFX...")
        @NotBlank(message = "p256dh는 필수입니다")
        String p256dh,

        @Schema(description = "인증 키", example = "tBHItJI5svbp...")
        @NotBlank(message = "auth는 필수입니다")
        String auth
    ) {}
}
