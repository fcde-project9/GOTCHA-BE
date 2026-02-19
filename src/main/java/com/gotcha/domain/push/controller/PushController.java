package com.gotcha.domain.push.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.push.dto.DeviceTokenRegisterRequest;
import com.gotcha.domain.push.dto.DeviceTokenUnregisterRequest;
import com.gotcha.domain.push.dto.PushSubscribeRequest;
import com.gotcha.domain.push.dto.PushUnsubscribeRequest;
import com.gotcha.domain.push.dto.VapidKeyResponse;
import com.gotcha.domain.push.service.PushNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
@Validated
public class PushController implements PushControllerApi {

    private final PushNotificationService pushNotificationService;

    @Override
    @GetMapping("/vapid-key")
    public ApiResponse<VapidKeyResponse> getVapidKey() {
        return ApiResponse.success(pushNotificationService.getVapidPublicKey());
    }

    @Override
    @PostMapping("/subscribe")
    public ApiResponse<Void> subscribe(@Valid @RequestBody PushSubscribeRequest request) {
        pushNotificationService.subscribe(request);
        return ApiResponse.success(null);
    }

    @Override
    @DeleteMapping("/subscribe")
    public ApiResponse<Void> unsubscribe(@Valid @RequestBody PushUnsubscribeRequest request) {
        pushNotificationService.unsubscribe(request.endpoint());
        return ApiResponse.success(null);
    }

    @Override
    @PostMapping("/register-device")
    public ApiResponse<Void> registerDevice(@Valid @RequestBody DeviceTokenRegisterRequest request) {
        pushNotificationService.registerDevice(request);
        return ApiResponse.success(null);
    }

    @Override
    @DeleteMapping("/register-device")
    public ApiResponse<Void> unregisterDevice(@Valid @RequestBody DeviceTokenUnregisterRequest request) {
        pushNotificationService.unregisterDevice(request.deviceToken());
        return ApiResponse.success(null);
    }
}
