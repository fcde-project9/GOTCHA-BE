package com.gotcha.domain.shop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gotcha._global.exception.BusinessException;
import com.gotcha._global.external.kakao.KakaoMapClient;
import com.gotcha.domain.favorite.repository.FavoriteRepository;
import com.gotcha.domain.shop.dto.ShopMapResponse;
import com.gotcha.domain.shop.dto.ShopSearchResultResponse;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.exception.ShopErrorCode;
import com.gotcha.domain.shop.repository.ShopRepository;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

    @Nested
    @DisplayName("searchShops - 가게 이름 검색")
    class SearchShops {

        @Test
        @DisplayName("lat/lng 없으면 DB 페이지네이션, distance null 반환")
        void searchShops_withoutLocation_distanceIsNull() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);
            given(shopRepository.searchByName("가챠", pageable))
                    .willReturn(new PageImpl<>(List.of(testShop), pageable, 1));

            // when
            ShopSearchResultResponse result = shopService.searchShops("가챠", null, null, pageable);

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).distance()).isNull();
            assertThat(result.totalCount()).isEqualTo(1);
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("lat/lng 있으면 전체 결과 거리순 정렬 후 페이지네이션")
        void searchShops_withLocation_sortedByDistance() {
            // given - 멀리 있는 가게와 가까운 가게
            Shop nearShop = Shop.builder()
                    .name("가까운 가챠샵").addressName("서울시 강남구").latitude(37.5172).longitude(127.0473).build();
            Shop farShop = Shop.builder()
                    .name("먼 가챠샵").addressName("부산시 해운대구").latitude(35.1628).longitude(129.1635).build();
            setShopId(nearShop, 2L);
            setShopId(farShop, 3L);

            PageRequest pageable = PageRequest.of(0, 20);
            given(shopRepository.searchByNameAll("가챠")).willReturn(List.of(farShop, nearShop)); // DB는 순서 무관

            // when - 강남 근처에서 검색
            ShopSearchResultResponse result = shopService.searchShops("가챠", 37.5172, 127.0473, pageable);

            // then - 가까운 순으로 정렬됨
            assertThat(result.content()).hasSize(2);
            assertThat(result.content().get(0).name()).isEqualTo("가까운 가챠샵");
            assertThat(result.content().get(1).name()).isEqualTo("먼 가챠샵");
            assertThat(result.content().get(0).distance()).isLessThan(result.content().get(1).distance());
        }

        @Test
        @DisplayName("lat/lng 있을 때 페이지네이션 - 2페이지 이상")
        void searchShops_withLocation_pagination() {
            // given - 3개 결과, size=2
            Shop shop1 = Shop.builder().name("가챠샵A").addressName("주소1").latitude(37.5172).longitude(127.0473).build();
            Shop shop2 = Shop.builder().name("가챠샵B").addressName("주소2").latitude(37.5180).longitude(127.0480).build();
            Shop shop3 = Shop.builder().name("가챠샵C").addressName("주소3").latitude(37.5200).longitude(127.0500).build();
            setShopId(shop1, 2L); setShopId(shop2, 3L); setShopId(shop3, 4L);

            given(shopRepository.searchByNameAll("가챠")).willReturn(List.of(shop1, shop2, shop3));

            // when
            ShopSearchResultResponse page0 = shopService.searchShops("가챠", 37.5172, 127.0473, PageRequest.of(0, 2));
            ShopSearchResultResponse page1 = shopService.searchShops("가챠", 37.5172, 127.0473, PageRequest.of(1, 2));

            // then
            assertThat(page0.content()).hasSize(2);
            assertThat(page0.totalCount()).isEqualTo(3);
            assertThat(page0.hasNext()).isTrue();
            assertThat(page1.content()).hasSize(1);
            assertThat(page1.hasNext()).isFalse();
        }

        @Test
        @DisplayName("lat만 있고 lng가 null이면 예외 발생")
        void searchShops_onlyLatProvided_throwsException() {
            // when & then
            assertThatThrownBy(() -> shopService.searchShops("가챠", 37.5172, null, PageRequest.of(0, 20)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ShopErrorCode.INVALID_COORDINATES.getMessage());
        }

        @Test
        @DisplayName("lng만 있고 lat가 null이면 예외 발생")
        void searchShops_onlyLngProvided_throwsException() {
            // when & then
            assertThatThrownBy(() -> shopService.searchShops("가챠", null, 127.0473, PageRequest.of(0, 20)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ShopErrorCode.INVALID_COORDINATES.getMessage());
        }

        @Test
        @DisplayName("좌표 범위를 벗어나면 예외 발생")
        void searchShops_outOfRangeCoordinates_throwsException() {
            // when & then
            assertThatThrownBy(() -> shopService.searchShops("가챠", 999.0, 999.0, PageRequest.of(0, 20)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ShopErrorCode.INVALID_COORDINATES.getMessage());
        }

        @Test
        @DisplayName("keyword가 null이면 예외 발생")
        void searchShops_nullKeyword_throwsException() {
            // when & then
            assertThatThrownBy(() -> shopService.searchShops(null, null, null, PageRequest.of(0, 20)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ShopErrorCode.INVALID_SEARCH_KEYWORD.getMessage());
        }

        @Test
        @DisplayName("keyword가 1자이면 정상 검색")
        void searchShops_oneCharKeyword_success() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);
            given(shopRepository.searchByName("가", pageable))
                    .willReturn(new PageImpl<>(List.of(testShop), pageable, 1));

            // when & then - 예외 없이 정상 동작
            ShopSearchResultResponse result = shopService.searchShops("가", null, null, pageable);
            assertThat(result.content()).hasSize(1);
        }

        @Test
        @DisplayName("keyword가 공백만이면 예외 발생")
        void searchShops_blankKeyword_throwsException() {
            // when & then
            assertThatThrownBy(() -> shopService.searchShops("  ", null, null, PageRequest.of(0, 20)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ShopErrorCode.INVALID_SEARCH_KEYWORD.getMessage());
        }

        @Test
        @DisplayName("검색 결과 없으면 빈 결과 반환")
        void searchShops_noResult_returnsEmpty() {
            // given
            PageRequest pageable = PageRequest.of(0, 20);
            given(shopRepository.searchByName("없는가게", pageable))
                    .willReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

            // when
            ShopSearchResultResponse result = shopService.searchShops("없는가게", null, null, pageable);

            // then
            assertThat(result.content()).isEmpty();
            assertThat(result.totalCount()).isZero();
            assertThat(result.hasNext()).isFalse();
        }
    }
}
