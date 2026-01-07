package com.gotcha.domain.shop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gotcha._global.external.kakao.KakaoMapClient;
import com.gotcha._global.external.kakao.dto.AddressInfo;
import com.gotcha.domain.shop.dto.NearbyShopResponse;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.exception.ShopException;
import com.gotcha.domain.shop.repository.ShopRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShopService {

    private final ShopRepository shopRepository;
    private final KakaoMapClient kakaoMapClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public Shop createShop(String name, Double latitude, Double longitude,
                           String mainImageUrl, String locationHint, Map<String, String> openTime) {
        log.info("=== createShop START ===");
        log.info("Input - name: {}, lat: {}, lng: {}, openTime: {}", name, latitude, longitude, openTime);

        try {
            validateCoordinates(latitude, longitude);
            log.info("Coordinates validated successfully");

            validateShopName(name);
            log.info("Shop name validated successfully");

            log.info("Calling Kakao API...");
            AddressInfo addressInfo = kakaoMapClient.getAddressInfo(latitude, longitude);
            log.info("AddressInfo received: {}", addressInfo);

            String openTimeJson = convertOpenTimeMapToString(openTime);

            log.info("Building Shop entity...");
            Shop shop = Shop.builder()
                    .name(name)
                    .addressName(addressInfo.addressName())
                    .latitude(latitude)
                    .longitude(longitude)
                    .mainImageUrl(mainImageUrl)
                    .locationHint(locationHint)
                    .openTime(openTimeJson)
                    .region1DepthName(addressInfo.region1DepthName())
                    .region2DepthName(addressInfo.region2DepthName())
                    .region3DepthName(addressInfo.region3DepthName())
                    .mainAddressNo(addressInfo.mainAddressNo())
                    .subAddressNo(addressInfo.subAddressNo())
                    .build();
            log.info("Shop entity built successfully");

            log.info("Saving to database...");
            Shop savedShop = shopRepository.save(shop);
            log.info("Shop saved successfully with ID: {}", savedShop.getId());

            return savedShop;
        } catch (Exception e) {
            log.error("Error in createShop: ", e);
            throw e;
        }
    }

    private String convertOpenTimeMapToString(Map<String, String> openTime) {
        if (openTime == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(openTime);
        } catch (JsonProcessingException e) {
            log.error("Error converting openTime to JSON string", e);
            throw new RuntimeException("Error converting openTime to JSON string", e);
        }
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw ShopException.invalidCoordinates();
        }
        if (latitude < -90 || latitude > 90) {
            throw ShopException.invalidCoordinates();
        }
        if (longitude < -180 || longitude > 180) {
            throw ShopException.invalidCoordinates();
        }
    }

    private void validateShopName(String name) {
        if (name == null || name.length() < 2 || name.length() > 100) {
            throw ShopException.invalidName();
        }
    }

    @Transactional(readOnly = true)
    public List<NearbyShopResponse> getNearbyShops(Double latitude, Double longitude) {
        log.info("getNearbyShops - lat: {}, lng: {}", latitude, longitude);

        // 좌표 검증 (기존 validateCoordinates 재사용)
        validateCoordinates(latitude, longitude);

        // 50m = 0.05km (Repository는 km 단위 사용)
        List<Shop> shops = shopRepository.findNearbyShops(latitude, longitude, 0.05);

        log.info("Found {} shops within 50m", shops.size());

        // Stream으로 DTO 변환
        return shops.stream()
                .map(NearbyShopResponse::from)
                .collect(Collectors.toList());
    }
}
