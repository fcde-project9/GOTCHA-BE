package com.gotcha.domain.shop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gotcha._global.external.kakao.KakaoMapClient;
import com.gotcha._global.external.kakao.dto.AddressInfo;
import com.gotcha.domain.favorite.repository.FavoriteRepository;
import com.gotcha.domain.shop.dto.NearbyShopResponse;
import com.gotcha.domain.shop.dto.OpenTimeDto;
import com.gotcha.domain.shop.dto.ShopMapResponse;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.exception.ShopException;
import com.gotcha.domain.shop.repository.ShopRepository;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final FavoriteRepository favoriteRepository;

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
    public List<NearbyShopResponse> checkNearbyShopsBeforeSave(Double latitude, Double longitude) {
        log.info("checkNearbyShopsBeforeSave - lat: {}, lng: {}", latitude, longitude);

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

    /**
     * 지도 영역 내 가게 목록 조회
     * @param northEastLat 북동쪽 위도
     * @param northEastLng 북동쪽 경도
     * @param southWestLat 남서쪽 위도
     * @param southWestLng 남서쪽 경도
     * @param centerLat 중심 위도 (거리 계산용)
     * @param centerLng 중심 경도 (거리 계산용)
     * @param userId 현재 로그인한 사용자 ID (null 가능)
     * @return 지도용 가게 응답 리스트 (거리순 정렬)
     */
    @Transactional(readOnly = true)
    public List<ShopMapResponse> getShopsInMap(
            Double northEastLat, Double northEastLng,
            Double southWestLat, Double southWestLng,
            Double centerLat, Double centerLng,
            Long userId) {

        log.info("getShopsInMap - bounds: NE({}, {}), SW({}, {}), center: ({}, {}), userId: {}",
                northEastLat, northEastLng, southWestLat, southWestLng, centerLat, centerLng, userId);

        // 좌표 검증
        validateCoordinates(northEastLat, northEastLng);
        validateCoordinates(southWestLat, southWestLng);
        validateCoordinates(centerLat, centerLng);

        // 경계 내 가게 조회
        List<Shop> shops = shopRepository.findShopsWithinBounds(
                northEastLat, northEastLng, southWestLat, southWestLng
        );

        log.info("Found {} shops within bounds", shops.size());

        // 찜 목록 조회 (로그인 사용자만)
        Set<Long> favoriteShopIds = Set.of();
        if (userId != null) {
            favoriteShopIds = favoriteRepository.findAllByUserId(userId).stream()
                    .map(favorite -> favorite.getShop().getId())
                    .collect(Collectors.toSet());
            log.info("User {} has {} favorite shops", userId, favoriteShopIds.size());
        }

        // 거리 계산 및 DTO 변환
        final Set<Long> finalFavoriteShopIds = favoriteShopIds;
        Map<Shop, Double> shopDistances = new HashMap<>();

        List<ShopMapResponse> responses = shops.stream()
                .peek(shop -> {
                    // 거리 계산 (km)
                    double distanceKm = calculateDistance(centerLat, centerLng,
                            shop.getLatitude(), shop.getLongitude());
                    shopDistances.put(shop, distanceKm);
                })
                .sorted(Comparator.comparing(shopDistances::get))  // 거리순 정렬
                .map(shop -> {
                    double distanceKm = shopDistances.get(shop);
                    String distanceStr = formatDistance(distanceKm);
                    boolean isFavorite = finalFavoriteShopIds.contains(shop.getId());
                    OpenTimeDto parsedOpenTime = parseOpenTime(shop.getOpenTime());
                    return ShopMapResponse.of(shop, distanceStr, parsedOpenTime, isFavorite);
                })
                .collect(Collectors.toList());

        log.info("Returning {} shops with distances calculated", responses.size());
        return responses;
    }

    /**
     * Haversine 공식을 사용하여 두 좌표 간 거리 계산
     * @param lat1 위도1
     * @param lng1 경도1
     * @param lat2 위도2
     * @param lng2 경도2
     * @return 거리 (km)
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int EARTH_RADIUS_KM = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * 거리를 50m 단위로 반올림하여 문자열로 변환
     * 1000m 미만: "50m", "100m", "950m"
     * 1000m 이상: "1.0km", "1.5km", "2.0km"
     * @param distanceKm 거리 (km)
     * @return 포맷된 거리 문자열
     */
    private String formatDistance(double distanceKm) {
        double distanceM = distanceKm * 1000;  // km -> m 변환

        // 50m 단위로 반올림
        double roundedM = Math.round(distanceM / 50.0) * 50.0;

        if (roundedM < 1000) {
            // 1000m 미만: "50m", "100m" 형식
            return String.format("%.0fm", roundedM);
        } else {
            // 1000m 이상: "1.0km", "1.5km" 형식
            double roundedKm = roundedM / 1000.0;
            return String.format("%.1fkm", roundedKm);
        }
    }

    /**
     * JSON 형식의 운영 시간 문자열을 OpenTimeDto로 파싱
     * @param openTimeJson JSON 문자열 (예: {"AM":"10:00", "PM":"20:00"})
     * @return 파싱된 OpenTimeDto 또는 파싱 실패 시 null
     */
    private OpenTimeDto parseOpenTime(String openTimeJson) {
        if (openTimeJson == null || openTimeJson.isEmpty()) {
            return null;
        }

        try {
            Map<String, String> timeMap = objectMapper.readValue(openTimeJson,
                    new TypeReference<Map<String, String>>() {});
            String amTime = timeMap.get("AM");
            String pmTime = timeMap.get("PM");

            if (amTime == null || pmTime == null) {
                log.warn("Missing AM or PM in openTime JSON: {}", openTimeJson);
                return null;
            }

            LocalTime openAm = LocalTime.parse(amTime);
            LocalTime closePm = LocalTime.parse(pmTime);

            return new OpenTimeDto(openAm, closePm);
        } catch (Exception e) {
            log.error("Error parsing openTime JSON: {}", openTimeJson, e);
            return null;
        }
    }
}
