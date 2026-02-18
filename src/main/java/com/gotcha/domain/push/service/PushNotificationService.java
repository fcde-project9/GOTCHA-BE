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
import jakarta.annotation.PostConstruct;
import java.security.GeneralSecurityException;
import java.security.Security;
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

/**
 * Web Push 알림 서비스
 *
 * Web Push API를 사용하여 브라우저로 푸시 알림을 전송합니다.
 * - VAPID(Voluntary Application Server Identification) 인증 방식 사용
 * - 사용자별 여러 기기(브라우저) 구독 지원 (1:N 관계)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PushNotificationService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final PushProperties pushProperties;
    private final SecurityUtil securityUtil;
    private final ObjectMapper objectMapper;

    // volatile: 멀티스레드 환경에서 가시성 보장 (Double-Checked Locking 패턴에 필수)
    private volatile PushService webPushService;

    /**
     * BouncyCastle 암호화 프로바이더 등록
     * - Web Push는 ECDH 키 교환과 AES-GCM 암호화를 사용
     * - Java 기본 프로바이더에서 지원하지 않는 알고리즘이 있어 BouncyCastle 필요
     * - 앱 시작 시 한 번만 등록 (중복 등록 방지)
     */
    @PostConstruct
    public void init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * VAPID 공개키 조회
     * - 프론트엔드에서 PushManager.subscribe() 호출 시 applicationServerKey로 사용
     * - 공개키가 설정되지 않은 경우 예외 발생
     */
    public VapidKeyResponse getVapidPublicKey() {
        String publicKey = pushProperties.getPublicKey();
        if (publicKey == null || publicKey.isBlank()) {
            throw PushException.vapidKeyNotConfigured();
        }
        return VapidKeyResponse.of(publicKey);
    }

    /**
     * 푸시 알림 구독 등록/갱신 (Upsert)
     * - 브라우저에서 PushManager.subscribe() 후 받은 구독 정보를 저장
     * - 동일 사용자+endpoint 조합이 있으면 키만 갱신 (브라우저가 키를 재생성할 수 있음)
     * - 없으면 새로 생성
     *
     * 소유권 검증: 현재 로그인한 사용자의 구독만 조회/갱신
     */
    @Transactional
    public void subscribe(PushSubscribeRequest request) {
        User user = securityUtil.getCurrentUser();
        Long userId = user.getId();

        // 현재 사용자 + endpoint 조합으로 기존 구독 조회 (소유권 검증 포함)
        Optional<PushSubscription> existing = pushSubscriptionRepository
                .findByUserIdAndEndpoint(userId, request.endpoint());

        if (existing.isPresent()) {
            // 기존 구독이 있으면 암호화 키만 갱신
            PushSubscription subscription = existing.get();
            subscription.updateKeys(request.keys().p256dh(), request.keys().auth());
            log.info("Push subscription updated - userId: {}, endpoint: {}", userId, request.endpoint());
        } else {
            // 새 구독 생성
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

    /**
     * 푸시 알림 구독 해제
     * - 사용자가 알림을 끄거나 로그아웃할 때 호출
     *
     * 소유권 검증: 현재 로그인한 사용자의 구독만 삭제 가능
     * (다른 사용자의 endpoint를 삭제하려고 하면 NOT_FOUND 반환)
     */
    @Transactional
    public void unsubscribe(String endpoint) {
        Long userId = securityUtil.getCurrentUserId();

        // 현재 사용자의 해당 endpoint 구독이 존재하는지 확인
        if (!pushSubscriptionRepository.existsByUserIdAndEndpoint(userId, endpoint)) {
            throw PushException.subscriptionNotFound(endpoint);
        }

        // 현재 사용자의 구독만 삭제
        pushSubscriptionRepository.deleteByUserIdAndEndpoint(userId, endpoint);
        log.info("Push subscription deleted - userId: {}, endpoint: {}", userId, endpoint);
    }

    /**
     * 특정 사용자에게 푸시 알림 전송
     * - 한 사용자가 여러 기기에서 구독했을 수 있으므로 모든 구독에 전송
     * - 구독이 없으면 조용히 종료 (에러 아님)
     */
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

    /**
     * 모든 구독자에게 푸시 알림 전송 (공지사항 등)
     */
    public void sendToAll(String title, String body, String url) {
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findAll();

        log.info("Sending push notification to all subscribers - count: {}", subscriptions.size());

        for (PushSubscription subscription : subscriptions) {
            sendPushNotification(subscription, title, body, url);
        }
    }

    /**
     * 실제 푸시 알림 전송 처리
     *
     * 동작 흐름:
     * 1. PushService 인스턴스 획득 (lazy initialization)
     * 2. 구독 정보로 Subscription 객체 생성
     * 3. 페이로드(title, body, url) JSON 직렬화
     * 4. 푸시 서버(FCM, Mozilla 등)로 전송
     * 5. 응답 상태에 따른 처리:
     *    - 410/404: 구독 만료됨 → DB에서 삭제
     *    - 4xx 이상: 전송 실패 로깅
     *    - 2xx: 성공
     */
    private void sendPushNotification(PushSubscription subscription, String title, String body, String url) {
        try {
            PushService pushService = getWebPushService();

            // DB에 저장된 구독 정보를 web-push 라이브러리 형식으로 변환
            Subscription webPushSubscription = new Subscription(
                    subscription.getEndpoint(),
                    new Subscription.Keys(subscription.getP256dh(), subscription.getAuth())
            );

            // 알림 페이로드 구성 (프론트엔드 Service Worker에서 파싱)
            Map<String, String> payload = Map.of(
                    "title", title,
                    "body", body,
                    "url", url != null ? url : "/"
            );

            String payloadJson = objectMapper.writeValueAsString(payload);

            // 푸시 서버로 전송 (VAPID 서명 포함)
            Notification notification = new Notification(webPushSubscription, payloadJson);
            HttpResponse response = pushService.send(notification);

            // 응답 처리
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 410 || statusCode == 404) {
                // 410 Gone / 404 Not Found: 구독이 더 이상 유효하지 않음
                // (사용자가 브라우저에서 알림 권한을 해제했거나 구독이 만료됨)
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
            // InterruptedException 발생 시 인터럽트 상태 복원 (Java 컨벤션)
            Thread.currentThread().interrupt();
            log.error("Push notification interrupted - endpoint: {}, error: {}",
                    subscription.getEndpoint(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending push notification - endpoint: {}, error: {}",
                    subscription.getEndpoint(), e.getMessage());
        }
    }

    /**
     * PushService 인스턴스 획득 (Lazy Initialization + Double-Checked Locking)
     *
     * 왜 이 패턴을 사용하는가:
     * 1. Lazy: VAPID 키가 설정되지 않은 환경에서도 앱이 시작되어야 함
     * 2. Thread-safe: 여러 스레드가 동시에 호출해도 인스턴스가 한 번만 생성됨
     * 3. Performance: 인스턴스 생성 후에는 synchronized 블록 진입 없이 바로 반환
     *
     * volatile + DCL 패턴:
     * - volatile: 다른 스레드에서 변경한 값이 즉시 보이도록 보장
     * - localRef: synchronized 블록 밖에서 volatile 읽기 최소화 (성능 최적화)
     */
    private PushService getWebPushService() throws GeneralSecurityException {
        PushService localRef = webPushService;
        if (localRef == null) {
            synchronized (this) {
                localRef = webPushService;
                if (localRef == null) {
                    // VAPID 키로 PushService 생성
                    // - publicKey: 프론트엔드에 노출되는 공개키
                    // - privateKey: 서버에서만 보관하는 비밀키 (VAPID 서명에 사용)
                    // - subject: mailto: 또는 https:// URL (푸시 서버가 연락할 수 있는 주소)
                    webPushService = localRef = new PushService(
                            pushProperties.getPublicKey(),
                            pushProperties.getPrivateKey(),
                            pushProperties.getSubject()
                    );
                }
            }
        }
        return localRef;
    }
}
