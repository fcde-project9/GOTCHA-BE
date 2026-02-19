package com.gotcha.domain.push.service;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gotcha._global.config.PushProperties;
import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.push.dto.DeviceTokenRegisterRequest;
import com.gotcha.domain.push.dto.PushSubscribeRequest;
import com.gotcha.domain.push.dto.VapidKeyResponse;
import com.gotcha.domain.push.entity.DevicePlatform;
import com.gotcha.domain.push.entity.DeviceToken;
import com.gotcha.domain.push.entity.PushSubscription;
import com.gotcha.domain.push.exception.PushException;
import com.gotcha.domain.push.repository.DeviceTokenRepository;
import com.gotcha.domain.push.repository.PushSubscriptionRepository;
import com.gotcha.domain.user.entity.User;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PushNotificationService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final PushProperties pushProperties;
    private final SecurityUtil securityUtil;
    private final ObjectMapper objectMapper;
    private final Executor taskExecutor;

    private volatile PushService webPushService;
    private volatile ApnsClient apnsClient;

    @PostConstruct
    public void init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @PreDestroy
    public void destroy() {
        ApnsClient client = apnsClient;
        if (client != null) {
            try {
                client.close().get();
            } catch (Exception e) {
                log.warn("Failed to close APNS client: {}", e.getMessage());
            }
        }
    }

    public VapidKeyResponse getVapidPublicKey() {
        PushProperties.Vapid vapid = pushProperties.getVapid();
        if (vapid == null) {
            throw PushException.vapidKeyNotConfigured();
        }
        String publicKey = vapid.getPublicKey();
        if (publicKey == null || publicKey.isBlank()) {
            throw PushException.vapidKeyNotConfigured();
        }
        return VapidKeyResponse.of(publicKey);
    }

    @Transactional
    public void subscribe(PushSubscribeRequest request) {
        User user = securityUtil.getCurrentUser();
        Long userId = user.getId();

        Optional<PushSubscription> existing = pushSubscriptionRepository
                .findByUserIdAndEndpoint(userId, request.endpoint());

        if (existing.isPresent()) {
            PushSubscription subscription = existing.get();
            subscription.updateKeys(request.keys().p256dh(), request.keys().auth());
            log.info("Push subscription updated - userId: {}, endpoint: {}", userId, request.endpoint());
        } else {
            PushSubscription subscription = PushSubscription.builder()
                    .user(user)
                    .endpoint(request.endpoint())
                    .p256dh(request.keys().p256dh())
                    .auth(request.keys().auth())
                    .build();
            pushSubscriptionRepository.save(subscription);
            log.info("Push subscription created - userId: {}, endpoint: {}", userId, request.endpoint());
        }
    }

    @Transactional
    public void unsubscribe(String endpoint) {
        Long userId = securityUtil.getCurrentUserId();

        if (!pushSubscriptionRepository.existsByUserIdAndEndpoint(userId, endpoint)) {
            throw PushException.subscriptionNotFound(endpoint);
        }

        pushSubscriptionRepository.deleteByUserIdAndEndpoint(userId, endpoint);
        log.info("Push subscription deleted - userId: {}, endpoint: {}", userId, endpoint);
    }

    @Transactional
    public void registerDevice(DeviceTokenRegisterRequest request) {
        User user = securityUtil.getCurrentUser();

        Optional<DeviceToken> existing = deviceTokenRepository.findByDeviceToken(request.deviceToken());

        if (existing.isPresent()) {
            existing.get().updateUserAndPlatform(user, request.platform());
            log.info("Device token ownership transferred - userId: {}, platform: {}", user.getId(),
                    request.platform());
        } else {
            DeviceToken deviceToken = DeviceToken.builder()
                    .user(user)
                    .deviceToken(request.deviceToken())
                    .platform(request.platform())
                    .build();
            deviceTokenRepository.save(deviceToken);
            log.info("Device token registered - userId: {}, platform: {}", user.getId(), request.platform());
        }
    }

    @Transactional
    public void unregisterDevice(String deviceToken) {
        Long userId = securityUtil.getCurrentUserId();

        if (!deviceTokenRepository.existsByUserIdAndDeviceToken(userId, deviceToken)) {
            throw PushException.deviceTokenNotFound();
        }

        deviceTokenRepository.deleteByUserIdAndDeviceToken(userId, deviceToken);
        log.info("Device token unregistered - userId: {}, token: {}", userId, deviceToken);
    }

    @Transactional
    public void sendToUser(Long userId, String title, String body, String url) {
        // Web Push
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findAllByUserId(userId);
        for (PushSubscription subscription : subscriptions) {
            sendWebPushNotification(subscription, title, body, url);
        }

        // APNS
        List<DeviceToken> iosDevices = deviceTokenRepository
                .findAllByUserIdAndPlatform(userId, DevicePlatform.IOS);
        for (DeviceToken device : iosDevices) {
            sendApnsNotification(device, title, body);
        }
    }

    @Transactional
    public void sendToAll(String title, String body, String url) {
        // Web Push
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findAll();
        log.info("Sending web push to all subscribers - count: {}", subscriptions.size());
        for (PushSubscription subscription : subscriptions) {
            sendWebPushNotification(subscription, title, body, url);
        }

        // APNS
        List<DeviceToken> iosDevices = deviceTokenRepository.findAllByPlatform(DevicePlatform.IOS);
        log.info("Sending APNS to all iOS devices - count: {}", iosDevices.size());
        for (DeviceToken device : iosDevices) {
            sendApnsNotification(device, title, body);
        }
    }

    private void sendWebPushNotification(PushSubscription subscription, String title, String body, String url) {
        try {
            PushService pushService = getWebPushService();

            Subscription webPushSubscription = new Subscription(
                    subscription.getEndpoint(),
                    new Subscription.Keys(subscription.getP256dh(), subscription.getAuth())
            );

            Map<String, String> payload = Map.of(
                    "title", title,
                    "body", body,
                    "url", url != null ? url : "/"
            );

            String payloadJson = objectMapper.writeValueAsString(payload);

            Notification notification = new Notification(webPushSubscription, payloadJson);
            HttpResponse response = pushService.send(notification);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 410 || statusCode == 404) {
                log.info("Push subscription expired, deleting - endpoint: {}", subscription.getEndpoint());
                pushSubscriptionRepository.deleteByEndpoint(subscription.getEndpoint());
            } else if (statusCode >= 400) {
                log.warn("Push notification failed - endpoint: {}, status: {}",
                        subscription.getEndpoint(), statusCode);
            } else {
                log.debug("Push notification sent - endpoint: {}", subscription.getEndpoint());
            }

        } catch (GeneralSecurityException | ExecutionException e) {
            log.error("Failed to send push notification - endpoint: {}, error: {}",
                    subscription.getEndpoint(), e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Push notification interrupted - endpoint: {}, error: {}",
                    subscription.getEndpoint(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending push notification - endpoint: {}, error: {}",
                    subscription.getEndpoint(), e.getMessage());
        }
    }

    private void sendApnsNotification(DeviceToken deviceToken, String title, String body) {
        try {
            ApnsClient client = getApnsClient();
            if (client == null) {
                log.debug("APNS client not configured, skipping native push");
                return;
            }

            PushProperties.Apns apns = pushProperties.getApns();
            if (apns == null) {
                log.debug("APNS properties not configured, skipping native push");
                return;
            }

            String payload = new SimpleApnsPayloadBuilder()
                    .setAlertTitle(title)
                    .setAlertBody(body)
                    .build();

            String sanitizedToken = TokenUtil.sanitizeTokenString(deviceToken.getDeviceToken());

            SimpleApnsPushNotification notification = new SimpleApnsPushNotification(
                    sanitizedToken,
                    apns.getBundleId(),
                    payload
            );

            client.sendNotification(notification).whenComplete((response, cause) -> {
                if (cause != null) {
                    log.error("APNS send failed - token: {}, error: {}",
                            deviceToken.getDeviceToken(), cause.getMessage());
                } else if (response.isAccepted()) {
                    log.debug("APNS notification sent - token: {}", deviceToken.getDeviceToken());
                } else {
                    String reason = response.getRejectionReason()
                            .map(Object::toString).orElse("Unknown");
                    log.warn("APNS rejected - token: {}, reason: {}", deviceToken.getDeviceToken(), reason);

                    if ("BadDeviceToken".equals(reason) || "Unregistered".equals(reason)) {
                        Long tokenId = deviceToken.getId();
                        String token = deviceToken.getDeviceToken();
                        taskExecutor.execute(() -> {
                            deviceTokenRepository.deleteById(tokenId);
                            log.info("Invalid APNS token deleted - token: {}", token);
                        });
                    }
                }
            });

        } catch (Exception e) {
            log.error("APNS send error - token: {}, error: {}",
                    deviceToken.getDeviceToken(), e.getMessage());
        }
    }

    private PushService getWebPushService() throws GeneralSecurityException {
        PushService localRef = webPushService;
        if (localRef == null) {
            synchronized (this) {
                localRef = webPushService;
                if (localRef == null) {
                    PushProperties.Vapid vapid = pushProperties.getVapid();
                    if (vapid == null) {
                        throw PushException.vapidKeyNotConfigured();
                    }
                    webPushService = localRef = new PushService(
                            vapid.getPublicKey(),
                            vapid.getPrivateKey(),
                            vapid.getSubject()
                    );
                }
            }
        }
        return localRef;
    }

    private ApnsClient getApnsClient() throws Exception {
        PushProperties.Apns apns = pushProperties.getApns();
        if (apns == null || apns.getPrivateKey() == null || apns.getPrivateKey().isBlank()) {
            return null;
        }

        ApnsClient localRef = apnsClient;
        if (localRef == null) {
            synchronized (this) {
                localRef = apnsClient;
                if (localRef == null) {
                    String apnsHost = apns.isProduction()
                            ? ApnsClientBuilder.PRODUCTION_APNS_HOST
                            : ApnsClientBuilder.DEVELOPMENT_APNS_HOST;

                    // 환경변수에서 PEM 키를 가져올 때 \n이 리터럴 문자열로 저장될 수 있으므로 변환
                    String privateKeyPem = apns.getPrivateKey().replace("\\n", "\n");
                    byte[] keyBytes = privateKeyPem.getBytes(StandardCharsets.UTF_8);
                    ApnsSigningKey signingKey = ApnsSigningKey.loadFromInputStream(
                            new ByteArrayInputStream(keyBytes),
                            apns.getTeamId(),
                            apns.getKeyId()
                    );

                    apnsClient = localRef = new ApnsClientBuilder()
                            .setApnsServer(apnsHost)
                            .setSigningKey(signingKey)
                            .build();
                }
            }
        }
        return localRef;
    }
}
