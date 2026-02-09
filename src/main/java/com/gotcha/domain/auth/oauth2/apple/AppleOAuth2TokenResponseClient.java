package com.gotcha.domain.auth.oauth2.apple;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Apple OAuth2 토큰 요청 클라이언트.
 *
 * Apple Token Endpoint 요청 시 동적으로 생성된 client_secret(JWT)을 주입합니다.
 * 다른 OAuth2 제공자(카카오, 구글, 네이버)는 기본 동작을 사용합니다.
 *
 * Spring Security 6.4+ RestClientAuthorizationCodeTokenResponseClient 사용.
 */
@Slf4j
@Component
public class AppleOAuth2TokenResponseClient implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

    private final RestClientAuthorizationCodeTokenResponseClient delegate;

    public AppleOAuth2TokenResponseClient(AppleClientSecretGenerator appleClientSecretGenerator) {
        this.delegate = new RestClientAuthorizationCodeTokenResponseClient();

        // Apple 요청에 대해 동적 client_secret 주입하는 컨버터 추가
        this.delegate.addParametersConverter(request -> {
            ClientRegistration clientRegistration = request.getClientRegistration();

            if (!"apple".equalsIgnoreCase(clientRegistration.getRegistrationId())) {
                // Apple이 아니면 추가 파라미터 없음
                return new LinkedMultiValueMap<>();
            }

            log.debug("Generating dynamic client_secret for Apple token request");

            String clientSecret = appleClientSecretGenerator.generateClientSecret();

            MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
            parameters.add("client_secret", clientSecret);

            return parameters;
        });
    }

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(
            OAuth2AuthorizationCodeGrantRequest authorizationCodeGrantRequest) {
        OAuth2AccessTokenResponse response = delegate.getTokenResponse(authorizationCodeGrantRequest);

        // Apple의 경우 refresh_token을 additionalParameters에 복사
        // Spring Security는 refresh_token을 표준 필드로 파싱하여 additionalParameters에 포함하지 않음
        // OidcUserRequest.getAdditionalParameters()에서 접근 가능하도록 명시적으로 추가
        String registrationId = authorizationCodeGrantRequest.getClientRegistration().getRegistrationId();
        if ("apple".equalsIgnoreCase(registrationId) && response.getRefreshToken() != null) {
            Map<String, Object> additionalParams = new HashMap<>(response.getAdditionalParameters());
            additionalParams.put("refresh_token", response.getRefreshToken().getTokenValue());

            return OAuth2AccessTokenResponse.withResponse(response)
                    .additionalParameters(additionalParams)
                    .build();
        }

        return response;
    }
}
