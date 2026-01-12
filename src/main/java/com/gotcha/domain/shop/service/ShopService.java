package com.gotcha.domain.shop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gotcha._global.external.kakao.KakaoMapClient;
import com.gotcha._global.external.kakao.dto.AddressInfo;
import com.gotcha.domain.favorite.repository.FavoriteRepository;
import com.gotcha.domain.review.dto.ReviewResponse;
import com.gotcha.domain.review.dto.ReviewSortType;
import com.gotcha.domain.review.entity.Review;
import com.gotcha.domain.review.entity.ReviewImage;
import com.gotcha.domain.review.repository.ReviewImageRepository;
import com.gotcha.domain.review.repository.ReviewLikeRepository;
import com.gotcha.domain.review.repository.ReviewRepository;
import com.gotcha.domain.shop.dto.NearbyShopResponse;
import com.gotcha.domain.shop.dto.NearbyShopsResponse;
import com.gotcha.domain.shop.dto.ShopDetailResponse;
import com.gotcha.domain.shop.dto.ShopMapResponse;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.exception.ShopException;
import com.gotcha.domain.shop.repository.ShopRepository;
import com.gotcha.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShopService {

    private final ShopRepository shopRepository;
    private final KakaoMapClient kakaoMapClient;
    private final ObjectMapper objectMapper;
    private final FavoriteRepository favoriteRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewLikeRepository reviewLikeRepository;

    @Transactional
    public Shop createShop(String name, Double latitude, Double longitude,
                           String mainImageUrl, String locationHint, Map<String, String> openTime,
                           User createdBy) {
        log.info("=== createShop START ===");
        log.info("Input - name: {}, lat: {}, lng: {}, openTime: {}, createdBy: {}",
                name, latitude, longitude, openTime, createdBy != null ? createdBy.getId() : "anonymous");

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
                    .createdBy(createdBy)
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
    public NearbyShopsResponse checkNearbyShopsBeforeSave(Double latitude, Double longitude) {
        log.info("checkNearbyShopsBeforeSave - lat: {}, lng: {}", latitude, longitude);

        // 좌표 검증 (기존 validateCoordinates 재사용)
        validateCoordinates(latitude, longitude);

        // 50m = 0.05km (Repository는 km 단위 사용)
        List<Shop> shops = shopRepository.findNearbyShops(latitude, longitude, 0.05);

        log.info("Found {} shops within 50m", shops.size());

        // Stream으로 DTO 변환
        List<NearbyShopResponse> shopResponses = shops.stream()
                .map(NearbyShopResponse::from)
                .collect(Collectors.toList());

        return NearbyShopsResponse.of(shopResponses);
    }

    /**
     * 지도 영역 내 가게 목록 조회
     *
     * @param northEastLat 북동쪽 위도 (필수)
     * @param northEastLng 북동쪽 경도 (필수)
     * @param southWestLat 남서쪽 위도 (필수)
     * @param southWestLng 남서쪽 경도 (필수)
     * @param latitude     사용자 현재 위치 위도 (선택, 거리 계산용, null이면 distance도 null 반환)
     * @param longitude    사용자 현재 위치 경도 (선택, 거리 계산용, null이면 distance도 null 반환)
     * @param user         현재 로그인한 사용자 (null 가능)
     * @return 지도용 가게 응답 리스트 (거리순 정렬, latitude/longitude가 null이면 distance는 null)
     */
    @Transactional(readOnly = true)
    public List<ShopMapResponse> getShopsInMap(
            Double northEastLat, Double northEastLng,
            Double southWestLat, Double southWestLng,
            Double latitude, Double longitude,
            User user) {

        Long userId = user != null ? user.getId() : null;
        log.info("getShopsInMap - bounds: NE({}, {}), SW({}, {}), userLocation: ({}, {}), userId: {}",
                northEastLat, northEastLng, southWestLat, southWestLng, latitude, longitude, userId);

        // 좌표 검증
        validateCoordinates(northEastLat, northEastLng);
        validateCoordinates(southWestLat, southWestLng);
        // latitude, longitude는 null 허용 (거리 계산용, null이면 distance를 null로 반환)
        if (latitude != null && longitude != null) {
            validateCoordinates(latitude, longitude);
        }

        // 경계 내 가게 조회
        List<Shop> shops = shopRepository.findShopsWithinBounds(
                northEastLat, northEastLng, southWestLat, southWestLng
        );

        log.info("Found {} shops within bounds", shops.size());

        // 찜 목록 조회 (로그인 사용자만, N+1 방지: JOIN FETCH 사용, 전체 조회)
        Set<Long> favoriteShopIds = Set.of();
        if (userId != null) {
            favoriteShopIds = favoriteRepository.findAllByUserIdWithShop(userId)
                    .stream()
                    .map(favorite -> favorite.getShop().getId())
                    .collect(Collectors.toSet());
            log.info("User {} has {} favorite shops", userId, favoriteShopIds.size());
        }

        // 거리 계산 및 DTO 변환
        final Set<Long> finalFavoriteShopIds = favoriteShopIds;
        Map<Shop, Double> shopDistances = new HashMap<>();

        List<ShopMapResponse> responses = shops.stream()
                .peek(shop -> {
                    // latitude 또는 longitude가 null이면 거리 계산 스킵
                    if (latitude != null && longitude != null) {
                        // 거리 계산 (km) - 사용자 위치 기준
                        double distanceKm = calculateDistance(latitude, longitude,
                                shop.getLatitude(), shop.getLongitude());
                        shopDistances.put(shop, distanceKm);
                    } else {
                        // 위치 정보가 없으면 거리를 null로 설정
                        shopDistances.put(shop, null);
                    }
                })
                .sorted(Comparator.comparing(shop -> {
                    Double distance = shopDistances.get(shop);
                    return distance != null ? distance : Double.MAX_VALUE;  // null은 맨 뒤로
                }))
                .map(shop -> {
                    Double distanceKm = shopDistances.get(shop);
                    String distanceStr = distanceKm != null ? formatDistance(distanceKm) : null;
                    boolean isFavorite = finalFavoriteShopIds.contains(shop.getId());
                    boolean openStatus = isOpenNow(shop.getOpenTime());
                    return ShopMapResponse.of(shop, distanceStr, openStatus, isFavorite);
                })
                .collect(Collectors.toList());

        log.info("Returning {} shops with distances calculated", responses.size());
        return responses;
    }

    /**
     * Haversine 공식을 사용하여 두 좌표 간 거리 계산
     *
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
     *
     * @param distanceKm 거리 (km)
     * @return 포맷된 거리 문자열
     */
    private String formatDistance(double distanceKm) {
        double distanceM = distanceKm * 1000;  // km -> m 변환

        // 50m 단위로 반올림
        double roundedM = Math.round(distanceM / 10.0) * 10.0;

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
     * openTime JSON 파싱
     *
     * @param openTimeJson JSON 문자열 (예: {"Mon":"10:00~22:00","Tue":null})
     * @return 파싱된 Map (파싱 실패 시 빈 Map)
     */
    private Map<String, String> parseOpenTime(String openTimeJson) {
        if (openTimeJson == null || openTimeJson.isEmpty()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(openTimeJson, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.error("Error parsing openTime JSON: {}", openTimeJson, e);
            return Map.of();
        }
    }

    /**
     * 현재 한국 시간 기준으로 영업중인지 판단
     *
     * @param openTimeJson JSON 문자열 (예: {"Mon":"10:00~22:00","Tue":null,"Wed":"10:00~22:00"})
     * @return 영업중이면 true, 아니면 false
     */
    public boolean isOpenNow(String openTimeJson) {
        Map<String, String> timeMap = parseOpenTime(openTimeJson);
        return isOpenNow(timeMap);
    }

    /**
     * 현재 한국 시간 기준으로 영업중인지 판단 (파싱된 Map 사용)
     *
     * @param timeMap 파싱된 영업 시간 Map
     * @return 영업중이면 true, 아니면 false
     */
    private boolean isOpenNow(Map<String, String> timeMap) {
        if (timeMap.isEmpty()) {
            return false;
        }

        try {
            // 현재 한국 시간 가져오기
            ZonedDateTime nowInKorea = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
            DayOfWeek dayOfWeek = nowInKorea.getDayOfWeek();
            LocalTime currentTime = nowInKorea.toLocalTime();

            // 요일을 "Mon", "Tue" 형식으로 변환
            String dayKey = getDayKey(dayOfWeek);

            String daySchedule = timeMap.get(dayKey);

            // 해당 요일이 null이거나 없으면 휴무
            if (daySchedule == null) {
                return false;
            }

            // "10:00~22:00" 형식 파싱
            if (!daySchedule.contains("~")) {
                log.warn("Invalid time format: {}", daySchedule);
                return false;
            }

            String[] times = daySchedule.split("~");
            if (times.length != 2) {
                log.warn("Invalid time format: {}", daySchedule);
                return false;
            }

            LocalTime openTime = LocalTime.parse(times[0].trim());
            LocalTime closeTime = LocalTime.parse(times[1].trim());

            // 익일 영업 (overnight) 처리 (예: 22:00~02:00)
            if (closeTime.isBefore(openTime)) {
                return !currentTime.isBefore(openTime) || !currentTime.isAfter(closeTime);
            }

            // 일반적인 경우 (예: 10:00~22:00)
            return !currentTime.isBefore(openTime) && !currentTime.isAfter(closeTime);

        } catch (Exception e) {
            log.error("Error checking open status", e);
            return false;
        }
    }

    /**
     * DayOfWeek를 "Mon", "Tue" 형식으로 변환
     *
     * @param dayOfWeek DayOfWeek
     * @return "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
     */
    private String getDayKey(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Mon";
            case TUESDAY -> "Tue";
            case WEDNESDAY -> "Wed";
            case THURSDAY -> "Thu";
            case FRIDAY -> "Fri";
            case SATURDAY -> "Sat";
            case SUNDAY -> "Sun";
        };
    }

    /**
     * 가게 상세 조회
     *
     * @param shopId 가게 ID
     * @param sortBy 리뷰 정렬 방식 (LATEST 또는 LIKE_COUNT)
     * @param user   현재 로그인한 사용자 (null 가능)
     * @return 가게 상세 정보 (찜 여부, 리뷰 5개 포함)
     */
    @Transactional(readOnly = true)
    public ShopDetailResponse getShopDetail(Long shopId, ReviewSortType sortBy, User user) {
        log.info("getShopDetail - shopId: {}, sortBy: {}, userId: {}",
                shopId, sortBy, user != null ? user.getId() : null);

        // 가게 조회
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> ShopException.notFound(shopId));

        // 찜 여부 확인 (로그인 사용자만)
        boolean isFavorite = false;
        if (user != null) {
            isFavorite = favoriteRepository.existsByUserIdAndShopId(user.getId(), shopId);
        }

        // openTime JSON 한 번만 파싱 (중복 파싱 방지)
        Map<String, String> openTimeMap = parseOpenTime(shop.getOpenTime());

        // 오늘의 영업 시간 추출
        String todayOpenTime = getTodayOpenTime(openTimeMap);

        // 영업 중 여부 확인
        boolean isOpen = isOpenNow(openTimeMap);

        // 리뷰 5개 조회 (정렬 적용)
        List<ReviewResponse> reviews = getTop5Reviews(shopId, sortBy, user);

        // 전체 리뷰 사진 개수 조회
        Long totalReviewImageCount = reviewImageRepository.countByShopId(shopId);

        // 최신 리뷰 이미지 4개 조회
        List<String> recentReviewImages = reviewImageRepository.findTop4ByShopId(shopId)
                .stream()
                .map(ReviewImage::getImageUrl)
                .toList();

        log.info("Shop detail - id: {}, reviews: {}, images: {}/{}",
                shopId, reviews.size(), recentReviewImages.size(), totalReviewImageCount);

        return ShopDetailResponse.of(shop, todayOpenTime, isOpen, isFavorite, reviews,
                totalReviewImageCount, recentReviewImages);
    }

    /**
     * 가게의 리뷰 상위 5개 조회
     *
     * @param shopId      가게 ID
     * @param sortBy      정렬 방식
     * @param currentUser 현재 사용자 (null 가능)
     * @return 리뷰 목록 (최대 5개)
     */
    private List<ReviewResponse> getTop5Reviews(Long shopId, ReviewSortType sortBy, User currentUser) {
        Pageable pageable = PageRequest.of(0, 5);
        Long currentUserId = currentUser != null ? currentUser.getId() : null;

        // 정렬 타입에 따라 다른 쿼리 호출
        Page<Review> reviewPage;
        if (sortBy == ReviewSortType.LIKE_COUNT) {
            reviewPage = reviewRepository.findAllByShopIdOrderByLikeCountDesc(shopId, pageable);
        } else {
            // 기본값: 최신순
            reviewPage = reviewRepository.findAllByShopIdOrderByCreatedAtDesc(shopId, pageable);
        }

        // N+1 방지: 이미지 일괄 조회
        List<Long> reviewIds = reviewPage.getContent().stream()
                .map(Review::getId)
                .toList();

        // 리뷰가 없으면 빈 리스트 반환
        if (reviewIds.isEmpty()) {
            return List.of();
        }

        Map<Long, List<ReviewImage>> imageMap = reviewImageRepository
                .findAllByReviewIdInOrderByReviewIdAscDisplayOrderAsc(reviewIds)
                .stream()
                .collect(Collectors.groupingBy(img -> img.getReview().getId()));

        // N+1 방지: 좋아요 수 일괄 조회 (배치 쿼리)
        Map<Long, Long> likeCountMap = reviewLikeRepository.countByReviewIdInGroupByReviewId(reviewIds)
                .stream()
                .collect(Collectors.toMap(
                        ReviewLikeRepository.ReviewLikeCount::getReviewId,
                        ReviewLikeRepository.ReviewLikeCount::getLikeCount
                ));

        // N+1 방지: 현재 사용자가 좋아요한 리뷰 목록 조회 (배치 쿼리)
        Set<Long> likedReviewIds = Set.of();
        if (currentUserId != null) {
            likedReviewIds = new HashSet<>(
                    reviewLikeRepository.findLikedReviewIds(currentUserId, reviewIds)
            );
        }
        final Set<Long> finalLikedReviewIds = likedReviewIds;

        return reviewPage.getContent().stream()
                .map(review -> ReviewResponse.from(
                        review,
                        review.getUser(),
                        imageMap.getOrDefault(review.getId(), List.of()),
                        currentUserId != null && review.getUser().getId().equals(currentUserId),
                        likeCountMap.getOrDefault(review.getId(), 0L),
                        finalLikedReviewIds.contains(review.getId())
                ))
                .toList();
    }

    /**
     * 오늘의 영업 시간 추출 (한국 시간 기준)
     *
     * @param openTimeJson JSON 문자열 (예: {"Mon":"10:00~22:00","Tue":null,"Wed":"10:00~22:00"})
     * @return 오늘의 영업 시간 (예: "10:00~22:00") 또는 null (휴무일)
     */
    private String getTodayOpenTime(String openTimeJson) {
        Map<String, String> timeMap = parseOpenTime(openTimeJson);
        return getTodayOpenTime(timeMap);
    }

    /**
     * 오늘의 영업 시간 추출 (파싱된 Map 사용)
     *
     * @param timeMap 파싱된 영업 시간 Map
     * @return 오늘의 영업 시간 (예: "10:00~22:00") 또는 null (휴무일)
     */
    private String getTodayOpenTime(Map<String, String> timeMap) {
        if (timeMap.isEmpty()) {
            return null;
        }

        try {
            // 현재 한국 시간 가져오기
            ZonedDateTime nowInKorea = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
            DayOfWeek dayOfWeek = nowInKorea.getDayOfWeek();

            // 요일을 "Mon", "Tue" 형식으로 변환
            String dayKey = getDayKey(dayOfWeek);

            // 해당 요일의 영업 시간 반환 (null이면 휴무)
            return timeMap.get(dayKey);

        } catch (Exception e) {
            log.error("Error extracting today's open time", e);
            return null;
        }
    }
}
