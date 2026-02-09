package com.gotcha.domain.auth.oauth2.apple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AppleOAuth2TokenResponseClientTest {

    @Mock
    private AppleClientSecretGenerator appleClientSecretGenerator;

    @Mock
    private RestClientAuthorizationCodeTokenResponseClient mockDelegate;

    private AppleOAuth2TokenResponseClient tokenResponseClient;

    @BeforeEach
    void setUp() {
        tokenResponseClient = new AppleOAuth2TokenResponseClient(appleClientSecretGenerator);
        ReflectionTestUtils.setField(tokenResponseClient, "delegate", mockDelegate);
    }

    @Nested
    @DisplayName("Apple 토큰 응답에서 refresh_token 전달")
    class AppleRefreshTokenTest {

        @Test
        @DisplayName("Apple 응답에 refresh_token이 있으면 additionalParameters에 추가한다")
        void apple_withRefreshToken_addsToAdditionalParameters() {
            // given
            OAuth2AuthorizationCodeGrantRequest request = createGrantRequest("apple");
            OAuth2AccessTokenResponse originalResponse = createTokenResponse(
                    "access-token-value", "apple-refresh-token", Map.of("id_token", "some-id-token"));
            given(mockDelegate.getTokenResponse(request)).willReturn(originalResponse);

            // when
            OAuth2AccessTokenResponse result = tokenResponseClient.getTokenResponse(request);

            // then
            assertThat(result.getAdditionalParameters()).containsEntry("refresh_token", "apple-refresh-token");
            assertThat(result.getAdditionalParameters()).containsEntry("id_token", "some-id-token");
            assertThat(result.getRefreshToken()).isNotNull();
            assertThat(result.getRefreshToken().getTokenValue()).isEqualTo("apple-refresh-token");
        }

        @Test
        @DisplayName("Apple 응답에 refresh_token이 없으면 그대로 반환한다")
        void apple_withoutRefreshToken_returnsAsIs() {
            // given
            OAuth2AuthorizationCodeGrantRequest request = createGrantRequest("apple");
            OAuth2AccessTokenResponse originalResponse = createTokenResponse(
                    "access-token-value", null, Map.of("id_token", "some-id-token"));
            given(mockDelegate.getTokenResponse(request)).willReturn(originalResponse);

            // when
            OAuth2AccessTokenResponse result = tokenResponseClient.getTokenResponse(request);

            // then
            assertThat(result.getAdditionalParameters()).doesNotContainKey("refresh_token");
            assertThat(result.getRefreshToken()).isNull();
        }
    }

    @Nested
    @DisplayName("비Apple 프로바이더 응답")
    class NonAppleProviderTest {

        @Test
        @DisplayName("카카오 응답은 수정하지 않고 그대로 반환한다")
        void kakao_returnsOriginalResponse() {
            // given
            OAuth2AuthorizationCodeGrantRequest request = createGrantRequest("kakao");
            OAuth2AccessTokenResponse originalResponse = createTokenResponse(
                    "kakao-access-token", "kakao-refresh-token", Map.of());
            given(mockDelegate.getTokenResponse(request)).willReturn(originalResponse);

            // when
            OAuth2AccessTokenResponse result = tokenResponseClient.getTokenResponse(request);

            // then
            assertThat(result).isSameAs(originalResponse);
        }

        @Test
        @DisplayName("구글 응답은 수정하지 않고 그대로 반환한다")
        void google_returnsOriginalResponse() {
            // given
            OAuth2AuthorizationCodeGrantRequest request = createGrantRequest("google");
            OAuth2AccessTokenResponse originalResponse = createTokenResponse(
                    "google-access-token", null, Map.of());
            given(mockDelegate.getTokenResponse(request)).willReturn(originalResponse);

            // when
            OAuth2AccessTokenResponse result = tokenResponseClient.getTokenResponse(request);

            // then
            assertThat(result).isSameAs(originalResponse);
        }
    }

    private OAuth2AuthorizationCodeGrantRequest createGrantRequest(String registrationId) {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId(registrationId)
                .clientId("test-client-id")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://example.com/callback")
                .authorizationUri("https://example.com/authorize")
                .tokenUri("https://example.com/token")
                .build();

        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client-id")
                .authorizationUri("https://example.com/authorize")
                .redirectUri("https://example.com/callback")
                .build();

        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success("auth-code")
                .redirectUri("https://example.com/callback")
                .build();

        OAuth2AuthorizationExchange exchange = new OAuth2AuthorizationExchange(
                authorizationRequest, authorizationResponse);

        return new OAuth2AuthorizationCodeGrantRequest(clientRegistration, exchange);
    }

    private OAuth2AccessTokenResponse createTokenResponse(
            String accessTokenValue, String refreshTokenValue, Map<String, Object> additionalParams) {
        OAuth2AccessTokenResponse.Builder builder = OAuth2AccessTokenResponse
                .withToken(accessTokenValue)
                .tokenType(OAuth2AccessToken.TokenType.BEARER)
                .expiresIn(3600)
                .scopes(Set.of("openid", "email"))
                .additionalParameters(new HashMap<>(additionalParams));

        if (refreshTokenValue != null) {
            builder.refreshToken(refreshTokenValue);
        }

        return builder.build();
    }
}
