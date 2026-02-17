package com.gotcha.domain.push.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gotcha._global.config.PushProperties;
import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.push.dto.PushSubscribeRequest;
import com.gotcha.domain.push.dto.VapidKeyResponse;
import com.gotcha.domain.push.entity.PushSubscription;
import com.gotcha.domain.push.exception.PushException;
import com.gotcha.domain.push.repository.PushSubscriptionRepository;
import com.gotcha.domain.user.entity.User;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
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
    private final PushProperties pushProperties;
    private final SecurityUtil securityUtil;
    private final ObjectMapper objectMapper;

    private PushService webPushService;

    public VapidKeyResponse getVapidPublicKey() {
        String publicKey = pushProperties.getPublicKey();
        if (publicKey == null || publicKey.isBlank()) {
            throw PushException.vapidKeyNotConfigured();
        }
        return VapidKeyResponse.of(publicKey);
    }

    @Transactional
    public void subscribe(PushSubscribeRequest request) {
        User user = securityUtil.getCurrentUser();

        Optional<PushSubscription> existing = pushSubscriptionRepository.findByEndpoint(request.endpoint());

        if (existing.isPresent()) {
            PushSubscription subscription = existing.get();
            subscription.updateKeys(request.keys().p256dh(), request.keys().auth());
            log.info("Push subscription updated - userId: {}, endpoint: {}", user.getId(), request.endpoint());
        } else {
            PushSubscription subscription = PushSubscription.builder()
                    .user(user)
                    .endpoint(request.endpoint())
                    .p256dh(request.keys().p256dh())
                    .auth(request.keys().auth())
                    .build();
            pushSubscriptionRepository.save(subscription);
            log.info("Push subscription created - userId: {}, endpoint: {}", user.getId(), request.endpoint());
        }
    }

    @Transactional
    public void unsubscribe(String endpoint) {
        Long userId = securityUtil.getCurrentUserId();

        if (!pushSubscriptionRepository.existsByEndpoint(endpoint)) {
            throw PushException.subscriptionNotFound(endpoint);
        }

        pushSubscriptionRepository.deleteByEndpoint(endpoint);
        log.info("Push subscription deleted - userId: {}, endpoint: {}", userId, endpoint);
    }

    public void sendToUser(Long userId, String title, String body, String url) {
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findAllByUserId(userId);

        if (subscriptions.isEmpty()) {
            log.debug("No push subscriptions found for userId: {}", userId);
            return;
        }

        for (PushSubscription subscription : subscriptions) {
            sendPushNotification(subscription, title, body, url);
        }
    }

    public void sendToAll(String title, String body, String url) {
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findAll();

        log.info("Sending push notification to all subscribers - count: {}", subscriptions.size());

        for (PushSubscription subscription : subscriptions) {
            sendPushNotification(subscription, title, body, url);
        }
    }

    private void sendPushNotification(PushSubscription subscription, String title, String body, String url) {
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
                deleteExpiredSubscription(subscription.getEndpoint());
            } else if (statusCode >= 400) {
                log.warn("Push notification failed - endpoint: {}, status: {}",
                        subscription.getEndpoint(), statusCode);
            } else {
                log.debug("Push notification sent - endpoint: {}", subscription.getEndpoint());
            }

        } catch (GeneralSecurityException | ExecutionException | InterruptedException e) {
            log.error("Failed to send push notification - endpoint: {}, error: {}",
                    subscription.getEndpoint(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending push notification - endpoint: {}, error: {}",
                    subscription.getEndpoint(), e.getMessage());
        }
    }

    @Transactional
    public void deleteExpiredSubscription(String endpoint) {
        pushSubscriptionRepository.deleteByEndpoint(endpoint);
    }

    private PushService getWebPushService() throws GeneralSecurityException {
        if (webPushService == null) {
            java.security.Security.addProvider(new BouncyCastleProvider());
            webPushService = new PushService(
                    pushProperties.getPublicKey(),
                    pushProperties.getPrivateKey(),
                    pushProperties.getSubject()
            );
        }
        return webPushService;
    }
}
