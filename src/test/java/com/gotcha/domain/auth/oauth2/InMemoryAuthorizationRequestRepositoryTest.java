package com.gotcha.domain.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("InMemoryAuthorizationRequestRepository 테스트")
class InMemoryAuthorizationRequestRepositoryTest {

    private InMemoryAuthorizationRequestRepository repository;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAuthorizationRequestRepository();
        ReflectionTestUtils.setField(repository, "allowedRedirectUrisString",
                "http://localhost:3000/oauth/callback,https://dev.gotcha.it.com/oauth/callback");
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Nested
    @DisplayName("인가 요청 저장 및 조회")
    class SaveAndLoad {

        @Test
        @DisplayName("저장 후 state로 조회 - 동일한 요청 반환")
        void saveAndLoad_returnsIdenticalRequest() {
            // given
            OAuth2AuthorizationRequest authRequest = createAuthorizationRequest("test-state");
            repository.saveAuthorizationRequest(authRequest, request, response);

            MockHttpServletRequest loadRequest = new MockHttpServletRequest();
            loadRequest.setParameter("state", "test-state");

            // when
            OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(loadRequest);

            // then
            assertThat(loaded).isNotNull();
            assertThat(loaded.getState()).isEqualTo("test-state");
            assertThat(loaded.getClientId()).isEqualTo(authRequest.getClientId());
            assertThat(loaded.getRedirectUri()).isEqualTo(authRequest.getRedirectUri());
        }

        @Test
        @DisplayName("state 파라미터 없음 - null 반환")
        void load_noState_returnsNull() {
            // when
            OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(request);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 state - null 반환")
        void load_unknownState_returnsNull() {
            // given
            request.setParameter("state", "unknown-state");

            // when
            OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(request);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("만료된 요청 - null 반환")
        void load_expiredRequest_returnsNull() throws Exception {
            // given
            OAuth2AuthorizationRequest authRequest = createAuthorizationRequest("expired-state");
            repository.saveAuthorizationRequest(authRequest, request, response);

            // 저장된 항목의 createdAt을 과거로 변경
            @SuppressWarnings("unchecked")
            var store = (java.util.concurrent.ConcurrentHashMap<String, InMemoryAuthorizationRequestRepository.StoredRequest>)
                    ReflectionTestUtils.getField(repository, "store");
            var expiredRequest = new InMemoryAuthorizationRequestRepository.StoredRequest(
                    authRequest, null, java.time.Instant.now().minusSeconds(300));
            store.put("expired-state", expiredRequest);

            MockHttpServletRequest loadRequest = new MockHttpServletRequest();
            loadRequest.setParameter("state", "expired-state");

            // when
            OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(loadRequest);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("인가 요청 삭제")
    class Remove {

        @Test
        @DisplayName("저장된 요청 삭제 - 요청 반환 후 메모리에서 제거")
        void remove_returnsRequestAndClearsStore() {
            // given
            OAuth2AuthorizationRequest authRequest = createAuthorizationRequest("remove-state");
            repository.saveAuthorizationRequest(authRequest, request, response);

            MockHttpServletRequest removeRequest = new MockHttpServletRequest();
            removeRequest.setParameter("state", "remove-state");

            // when
            OAuth2AuthorizationRequest removed = repository.removeAuthorizationRequest(
                    removeRequest, new MockHttpServletResponse());

            // then
            assertThat(removed).isNotNull();
            assertThat(removed.getState()).isEqualTo("remove-state");

            // 재조회 시 null
            assertThat(repository.loadAuthorizationRequest(removeRequest)).isNull();
        }

        @Test
        @DisplayName("state 없는 삭제 요청 - null 반환")
        void remove_noState_returnsNull() {
            // when
            OAuth2AuthorizationRequest result = repository.removeAuthorizationRequest(
                    request, response);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("null 인가 요청 저장 시 기존 항목 삭제")
        void save_null_removesExisting() {
            // given
            OAuth2AuthorizationRequest authRequest = createAuthorizationRequest("null-test");
            repository.saveAuthorizationRequest(authRequest, request, response);

            MockHttpServletRequest removeRequest = new MockHttpServletRequest();
            removeRequest.setParameter("state", "null-test");

            // when
            repository.saveAuthorizationRequest(null, removeRequest, response);

            // then
            assertThat(repository.loadAuthorizationRequest(removeRequest)).isNull();
        }
    }

    @Nested
    @DisplayName("redirect_uri 처리")
    class RedirectUri {

        @Test
        @DisplayName("유효한 redirect_uri - 저장 후 조회 가능")
        void validRedirectUri_savedAndRetrievable() {
            // given
            OAuth2AuthorizationRequest authRequest = createAuthorizationRequest("redirect-state");
            request.setParameter("redirect_uri", "http://localhost:3000/oauth/callback");
            repository.saveAuthorizationRequest(authRequest, request, response);

            MockHttpServletRequest loadRequest = new MockHttpServletRequest();
            loadRequest.setParameter("state", "redirect-state");

            // when
            String redirectUri = repository.getRedirectUri(loadRequest);

            // then
            assertThat(redirectUri).isEqualTo("http://localhost:3000/oauth/callback");
        }

        @Test
        @DisplayName("유효하지 않은 redirect_uri - 저장되지 않음")
        void invalidRedirectUri_notSaved() {
            // given
            OAuth2AuthorizationRequest authRequest = createAuthorizationRequest("invalid-redirect");
            request.setParameter("redirect_uri", "https://evil-site.com/callback");
            repository.saveAuthorizationRequest(authRequest, request, response);

            MockHttpServletRequest loadRequest = new MockHttpServletRequest();
            loadRequest.setParameter("state", "invalid-redirect");

            // when
            String redirectUri = repository.getRedirectUri(loadRequest);

            // then
            assertThat(redirectUri).isNull();
        }

        @Test
        @DisplayName("redirect_uri 없음 - null 반환")
        void noRedirectUri_returnsNull() {
            // given
            OAuth2AuthorizationRequest authRequest = createAuthorizationRequest("no-redirect");
            repository.saveAuthorizationRequest(authRequest, request, response);

            MockHttpServletRequest loadRequest = new MockHttpServletRequest();
            loadRequest.setParameter("state", "no-redirect");

            // when
            String redirectUri = repository.getRedirectUri(loadRequest);

            // then
            assertThat(redirectUri).isNull();
        }

        @Test
        @DisplayName("state 없이 조회 - null 반환")
        void getRedirectUri_noState_returnsNull() {
            // when
            String result = repository.getRedirectUri(request);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("화이트리스트 검증")
    class WhitelistValidation {

        @Test
        @DisplayName("화이트리스트에 포함된 URI - 유효함")
        void validRedirectUri_inWhitelist() {
            assertThat(repository.isValidRedirectUri("http://localhost:3000/oauth/callback")).isTrue();
        }

        @Test
        @DisplayName("화이트리스트에 포함된 두 번째 URI - 유효함")
        void validRedirectUri_secondInWhitelist() {
            assertThat(repository.isValidRedirectUri("https://dev.gotcha.it.com/oauth/callback")).isTrue();
        }

        @Test
        @DisplayName("화이트리스트에 없는 URI - 유효하지 않음")
        void invalidRedirectUri_notInWhitelist() {
            assertThat(repository.isValidRedirectUri("https://evil-site.com/callback")).isFalse();
        }

        @Test
        @DisplayName("null URI - 유효하지 않음")
        void invalidRedirectUri_null() {
            assertThat(repository.isValidRedirectUri(null)).isFalse();
        }

        @Test
        @DisplayName("빈 문자열 URI - 유효하지 않음")
        void invalidRedirectUri_blank() {
            assertThat(repository.isValidRedirectUri("   ")).isFalse();
        }

        @Test
        @DisplayName("부분 일치하는 URI - 유효하지 않음")
        void invalidRedirectUri_partialMatch() {
            assertThat(repository.isValidRedirectUri("http://localhost:3000/oauth/callback/extra")).isFalse();
        }

        @Test
        @DisplayName("공백이 포함된 화이트리스트 - trim 처리")
        void whitelist_withSpaces_trimmed() {
            ReflectionTestUtils.setField(repository, "allowedRedirectUrisString",
                    " http://localhost:3000/oauth/callback , https://dev.gotcha.it.com/oauth/callback ");

            assertThat(repository.isValidRedirectUri("http://localhost:3000/oauth/callback")).isTrue();
            assertThat(repository.isValidRedirectUri("https://dev.gotcha.it.com/oauth/callback")).isTrue();
        }
    }

    @Nested
    @DisplayName("만료 항목 정리")
    class Eviction {

        @Test
        @DisplayName("만료된 항목만 정리")
        void evictExpiredEntries_removesOnlyExpired() {
            // given
            OAuth2AuthorizationRequest valid = createAuthorizationRequest("valid-state");
            OAuth2AuthorizationRequest expired = createAuthorizationRequest("expired-state");
            repository.saveAuthorizationRequest(valid, request, response);
            repository.saveAuthorizationRequest(expired, request, response);

            // expired 항목의 createdAt을 과거로 변경
            @SuppressWarnings("unchecked")
            var store = (java.util.concurrent.ConcurrentHashMap<String, InMemoryAuthorizationRequestRepository.StoredRequest>)
                    ReflectionTestUtils.getField(repository, "store");
            var expiredStored = new InMemoryAuthorizationRequestRepository.StoredRequest(
                    expired, null, java.time.Instant.now().minusSeconds(300));
            store.put("expired-state", expiredStored);

            // when
            repository.evictExpiredEntries();

            // then
            assertThat(repository.getStoreSize()).isEqualTo(1);

            MockHttpServletRequest validRequest = new MockHttpServletRequest();
            validRequest.setParameter("state", "valid-state");
            assertThat(repository.loadAuthorizationRequest(validRequest)).isNotNull();
        }
    }

    private OAuth2AuthorizationRequest createAuthorizationRequest(String state) {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://appleid.apple.com/auth/authorize")
                .clientId("test-client-id")
                .redirectUri("http://localhost:8080/api/auth/callback/apple")
                .state(state)
                .build();
    }
}
