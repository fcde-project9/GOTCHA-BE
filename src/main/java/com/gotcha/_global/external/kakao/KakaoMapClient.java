package com.gotcha._global.external.kakao;

import com.gotcha._global.external.kakao.dto.AddressInfo;
import com.gotcha._global.external.kakao.dto.KakaoAddressResponse;
import com.gotcha.domain.shop.exception.ShopException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoMapClient {

    private static final String COORD_TO_ADDRESS_PATH = "/v2/local/geo/coord2address.json";

    private final RestTemplate restTemplate;

    @Value("${kakao.api.rest-api-key}")
    private String restApiKey;

    @Value("${kakao.api.base-url}")
    private String baseUrl;

    public KakaoAddressResponse convertCoordinateToAddress(Double longitude, Double latitude) {
        String url = buildUrl(longitude, latitude);
        log.info("Kakao API URL: {}", url);

        HttpHeaders headers = createHeaders();
        log.info("Request headers prepared");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            log.info("Calling Kakao API...");
            ResponseEntity<KakaoAddressResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    KakaoAddressResponse.class
            );

            log.info("Kakao API response status: {}", response.getStatusCode());
            KakaoAddressResponse body = response.getBody();
            log.info("Response body: {}", body);

            if (body == null || !body.hasResult()) {
                log.warn("No address found for coordinates: lat={}, lng={}", latitude, longitude);
                throw ShopException.addressNotFound(latitude, longitude);
            }

            log.info("Successfully retrieved address from Kakao API");
            return body;

        } catch (RestClientException e) {
            log.error("Kakao API call failed: lat={}, lng={}", latitude, longitude, e);
            throw ShopException.kakaoApiError(e.getMessage());
        }
    }

    private String buildUrl(Double longitude, Double latitude) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl + COORD_TO_ADDRESS_PATH)
                .queryParam("x", longitude)
                .queryParam("y", latitude)
                .toUriString();
    }

    public AddressInfo getAddressInfo(Double latitude, Double longitude) {
        KakaoAddressResponse response = convertCoordinateToAddress(longitude, latitude);
        AddressInfo addressInfo = AddressInfo.from(response);

        if (addressInfo == null) {
            throw ShopException.addressNotFound(latitude, longitude);
        }

        return addressInfo;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + restApiKey);
        return headers;
    }
}
