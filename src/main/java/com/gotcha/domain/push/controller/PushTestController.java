package com.gotcha.domain.push.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.push.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("local")
@RestController
@RequestMapping("/api/push/test")
@RequiredArgsConstructor
public class PushTestController {

    private final PushNotificationService pushNotificationService;
    private final SecurityUtil securityUtil;

    @PostMapping("/send-me")
    public ApiResponse<Void> sendToMe(
            @RequestParam(defaultValue = "GOTCHA 테스트") String title,
            @RequestParam(defaultValue = "푸시 알림이 정상 동작합니다!") String body) {
        Long userId = securityUtil.getCurrentUserId();
        pushNotificationService.sendToUser(userId, title, body, "/");
        return ApiResponse.success(null);
    }
}
