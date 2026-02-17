package com.gotcha.domain.push.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "푸시 알림 구독 해제 요청")
public record PushUnsubscribeRequest(

    @Schema(description = "브라우저 푸시 엔드포인트 URL", example = "https://fcm.googleapis.com/fcm/send/...")
    @NotBlank(message = "endpoint는 필수입니다")
    String endpoint
) {}
