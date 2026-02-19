package com.gotcha.domain.push.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.push.dto.DeviceTokenRegisterRequest;
import com.gotcha.domain.push.dto.DeviceTokenUnregisterRequest;
import com.gotcha.domain.push.dto.PushSubscribeRequest;
import com.gotcha.domain.push.dto.PushUnsubscribeRequest;
import com.gotcha.domain.push.dto.VapidKeyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Push Notification", description = "푸시 알림 API")
public interface PushControllerApi {

    @Operation(
        summary = "VAPID 공개키 조회",
        description = "Web Push 구독에 필요한 VAPID 공개키를 반환합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "VAPID 키 미설정 (P003)"
        )
    })
    ApiResponse<VapidKeyResponse> getVapidKey();

    @Operation(
        summary = "푸시 알림 구독",
        description = "브라우저 푸시 알림을 구독합니다. 동일한 endpoint가 이미 존재하면 키를 갱신합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "구독 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패 (A001)"
        )
    })
    ApiResponse<Void> subscribe(PushSubscribeRequest request);

    @Operation(
        summary = "푸시 알림 구독 해제",
        description = "브라우저 푸시 알림 구독을 해제합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "구독 해제 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패 (A001)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "구독 정보 없음 (P001)"
        )
    })
    ApiResponse<Void> unsubscribe(PushUnsubscribeRequest request);

    @Operation(
        summary = "네이티브 푸시 기기 등록",
        description = "APNS/FCM 디바이스 토큰을 등록합니다. 동일한 토큰이 이미 존재하면 소유자를 현재 사용자로 변경합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "기기 등록 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패 (A001)"
        )
    })
    ApiResponse<Void> registerDevice(DeviceTokenRegisterRequest request);

    @Operation(
        summary = "네이티브 푸시 기기 해제",
        description = "등록된 APNS/FCM 디바이스 토큰을 해제합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "기기 해제 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패 (A001)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "기기 토큰 없음 (P004)"
        )
    })
    ApiResponse<Void> unregisterDevice(DeviceTokenUnregisterRequest request);
}
