package com.gotcha.domain.shop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gotcha._global.external.kakao.KakaoMapClient;
import com.gotcha.domain.favorite.repository.FavoriteRepository;
import com.gotcha.domain.shop.dto.ShopMapResponse;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.repository.ShopRepository;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShopServiceTest {

    @InjectMocks
    private ShopService shopService;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private KakaoMapClient kakaoMapClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private com.gotcha.domain.review.repository.ReviewRepository reviewRepository;

    @Mock
    private com.gotcha.domain.review.repository.ReviewImageRepository reviewImageRepository;

    @Mock
    private com.gotcha.domain.review.repository.ReviewLikeRepository reviewLikeRepository;

    private User testUser;
    private Shop testShop;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("12345")
                .nickname("테스트유저")
                .build();
        setUserId(testUser, 1L);

        testShop = Shop.builder()
                .name("테스트 가게")
                .addressName("서울시 강남구 신사동 123-45")
                .latitude(37.5172)
                .longitude(127.0473)
                .mainImageUrl("https://example.com/image.jpg")
                .region1DepthName("서울")
                .region2DepthName("강남구")
                .region3DepthName("신사동")
                .mainAddressNo("123")
                .subAddressNo("45")
                .build();
        setShopId(testShop, 1L);
    }

    private void setUserId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setShopId(Shop shop, Long id) {
        try {
            var field = Shop.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(shop, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("getShopsInMap - 지도 영역 내 가게 조회")
    class GetShopsInMap {

        @Test
        @DisplayName("centerLat, centerLng가 모두 있으면 거리 계산하여 반환")
        void withValidCoordinates_calculatesDistance() {
            // given
            Double northEastLat = 37.52;
            Double northEastLng = 127.05;
            Double southWestLat = 37.51;
            Double southWestLng = 127.04;
            Double centerLat = 37.515;
            Double centerLng = 127.045;

            given(shopRepository.findShopsWithinBounds(northEastLat, northEastLng, southWestLat, southWestLng))
                    .willReturn(List.of(testShop));
            given(favoriteRepository.findAllByUserIdWithShop(testUser.getId()))
                    .willReturn(Collections.emptyList());

            // when
            List<ShopMapResponse> result = shopService.getShopsInMap(
                    northEastLat, northEastLng, southWestLat, southWestLng,
                    centerLat, centerLng, testUser);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).distance()).isNotNull();
            assertThat(result.get(0).distance()).isNotEmpty();
        }

        @Test
        @DisplayName("centerLat가 null이면 distance를 null로 반환")
        void withNullCenterLat_returnsNullDistance() {
            // given
            Double northEastLat = 37.52;
            Double northEastLng = 127.05;
            Double southWestLat = 37.51;
            Double southWestLng = 127.04;
            Double centerLat = null;
            Double centerLng = 127.045;

            given(shopRepository.findShopsWithinBounds(northEastLat, northEastLng, southWestLat, southWestLng))
                    .willReturn(List.of(testShop));
            given(favoriteRepository.findAllByUserIdWithShop(testUser.getId()))
                    .willReturn(Collections.emptyList());

            // when
            List<ShopMapResponse> result = shopService.getShopsInMap(
                    northEastLat, northEastLng, southWestLat, southWestLng,
                    centerLat, centerLng, testUser);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).distance()).isNull();
        }

        @Test
        @DisplayName("centerLng가 null이면 distance를 null로 반환")
        void withNullCenterLng_returnsNullDistance() {
            // given
            Double northEastLat = 37.52;
            Double northEastLng = 127.05;
            Double southWestLat = 37.51;
            Double southWestLng = 127.04;
            Double centerLat = 37.515;
            Double centerLng = null;

            given(shopRepository.findShopsWithinBounds(northEastLat, northEastLng, southWestLat, southWestLng))
                    .willReturn(List.of(testShop));
            given(favoriteRepository.findAllByUserIdWithShop(testUser.getId()))
                    .willReturn(Collections.emptyList());

            // when
            List<ShopMapResponse> result = shopService.getShopsInMap(
                    northEastLat, northEastLng, southWestLat, southWestLng,
                    centerLat, centerLng, testUser);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).distance()).isNull();
        }

        @Test
        @DisplayName("centerLat, centerLng 모두 null이면 distance를 null로 반환")
        void withBothCenterCoordinatesNull_returnsNullDistance() {
            // given
            Double northEastLat = 37.52;
            Double northEastLng = 127.05;
            Double southWestLat = 37.51;
            Double southWestLng = 127.04;
            Double centerLat = null;
            Double centerLng = null;

            given(shopRepository.findShopsWithinBounds(northEastLat, northEastLng, southWestLat, southWestLng))
                    .willReturn(List.of(testShop));
            given(favoriteRepository.findAllByUserIdWithShop(testUser.getId()))
                    .willReturn(Collections.emptyList());

            // when
            List<ShopMapResponse> result = shopService.getShopsInMap(
                    northEastLat, northEastLng, southWestLat, southWestLng,
                    centerLat, centerLng, testUser);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).distance()).isNull();
        }

        @Test
        @DisplayName("비로그인 사용자(user=null)도 정상 조회 가능")
        void withNullUser_returnsShops() {
            // given
            Double northEastLat = 37.52;
            Double northEastLng = 127.05;
            Double southWestLat = 37.51;
            Double southWestLng = 127.04;
            Double centerLat = 37.515;
            Double centerLng = 127.045;

            given(shopRepository.findShopsWithinBounds(northEastLat, northEastLng, southWestLat, southWestLng))
                    .willReturn(List.of(testShop));

            // when
            List<ShopMapResponse> result = shopService.getShopsInMap(
                    northEastLat, northEastLng, southWestLat, southWestLng,
                    centerLat, centerLng, null);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).isFavorite()).isFalse();
            assertThat(result.get(0).distance()).isNotNull();
        }
    }
}
