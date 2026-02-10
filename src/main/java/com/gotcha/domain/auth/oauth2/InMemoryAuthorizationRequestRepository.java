package com.gotcha.domain.auth.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * OAuth2 인가 요청을 서버 메모리에 저장하는 Repository.
 *
 * 쿠키 기반 저장소의 한계(Apple form_post에서 브라우저가 cross-site 쿠키 차단)를 해결하기 위해
 * ConcurrentHashMap에 state를 키로 인가 요청을 저장합니다.
 */
@Slf4j
@Component
public class InMemoryAuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final int EXPIRE_SECONDS = 180;
    private static final int MAX_ENTRIES = 10_000;

    private final ConcurrentHashMap<String, StoredRequest> store = new ConcurrentHashMap<>();

    @Value("${oauth2.allowed-redirect-uris:http://localhost:3000/oauth/callback}")
    private String allowedRedirectUrisString;

        @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String state = request.getParameter("state");
        if (state == null) {
            log.debug("No state parameter in request");
            return null;
        }

        StoredRequest stored = store.get(state);
        if (stored == null) {
            log.debug("No authorization request found for state: {}", state);
            return null;
        }

        if (stored.isExpired()) {
            store.remove(state);
            log.debug("Authorization request expired for state: {}", state);
            return null;
        }

        return stored.authorizationRequest();
    }

        @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            String state = request.getParameter("state");
            if (state != null) {
                store.remove(state);
            }
            return;
        }

        if (store.size() >= MAX_ENTRIES) {
            evictExpiredEntries();
            if (store.size() >= MAX_ENTRIES) {
                log.warn("Authorization request store is full ({} entries)", store.size());
            }
        }

        String state = authorizationRequest.getState();
        String redirectUri = resolveRedirectUri(request);

        store.put(state, new StoredRequest(authorizationRequest, redirectUri, Instant.now()));
        log.debug("Saved authorization request with state: {}", state);
    }

        @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        String state = request.getParameter("state");
        if (state == null) {
            return null;
        }

        StoredRequest stored = store.remove(state);
        if (stored == null) {
            return null;
        }

        if (stored.isExpired()) {
            log.debug("Removed expired authorization request with state: {}", state);
            return null;
        }

        // 핸들러에서 redirect_uri를 조회할 수 있도록 request attribute에 저장
        if (stored.redirectUri() != null) {
            request.setAttribute("oauth2_redirect_uri", stored.redirectUri());
        }

        log.debug("Removed authorization request with state: {}", state);
        return stored.authorizationRequest();
    }

    /**
     * 콜백 시 저장된 redirect_uri를 조회 (state 파라미터 기반)
     */
        public String getRedirectUri(HttpServletRequest request) {
        // removeAuthorizationRequest에서 저장한 attribute 우선 조회
        String attrUri = (String) request.getAttribute("oauth2_redirect_uri");
        if (attrUri != null) {
            return attrUri;
        }

        String state = request.getParameter("state");
        if (state == null) {
            return null;
        }

        StoredRequest stored = store.get(state);
        if (stored == null || stored.isExpired()) {
            return null;
        }

        return stored.redirectUri();
    }

    /**
     * redirect_uri가 화이트리스트에 포함되어 있는지 검증
     */
        boolean isValidRedirectUri(String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank()) {
            return false;
        }
        return getAllowedRedirectUris().stream()
                .anyMatch(redirectUri::equals);
    }

    /**
     * 만료된 항목을 주기적으로 정리 (60초마다)
     */
        @Scheduled(fixedRate = 60_000)
    public void evictExpiredEntries() {
        int before = store.size();
        Iterator<Map.Entry<String, StoredRequest>> it = store.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isExpired()) {
                it.remove();
            }
        }
        int removed = before - store.size();
        if (removed > 0) {
            log.debug("Evicted {} expired authorization requests", removed);
        }
    }

    private String resolveRedirectUri(HttpServletRequest request) {
        String redirectUri = request.getParameter("redirect_uri");
        if (redirectUri != null && !redirectUri.isBlank() && isValidRedirectUri(redirectUri)) {
            log.debug("Saved redirect_uri: {}", redirectUri);
            return redirectUri;
        }
        if (redirectUri != null && !redirectUri.isBlank()) {
            log.warn("Invalid redirect_uri blocked: {}", redirectUri);
        }
        return null;
    }

    private List<String> getAllowedRedirectUris() {
        return Arrays.stream(allowedRedirectUrisString.split(","))
                .map(String::trim)
                .filter(uri -> !uri.isEmpty())
                .toList();
    }

    // 패키지 접근: 테스트용
    int getStoreSize() {
        return store.size();
    }

    record StoredRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            String redirectUri,
            Instant createdAt
    ) {
        boolean isExpired() {
            return Instant.now().isAfter(createdAt.plusSeconds(EXPIRE_SECONDS));
        }
    }
}
