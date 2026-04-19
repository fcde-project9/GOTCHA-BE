package com.gotcha.domain.shop.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gotcha.config.TestcontainersConfig;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
class ShopRepositoryTest {

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    private User creator;

    @BeforeEach
    void setUp() {
        creator = userRepository.save(User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("creator123")
                .nickname("제보자")
                .build());
    }

    @Test
    @DisplayName("ID로 샵 조회 시 생성자 정보 함께 로드")
    void findByIdWithCreator() {
        // given
        Shop shop = Shop.builder()
                .name("가챠샵")
                .addressName("서울시 강남구")
                .latitude(37.4979)
                .longitude(127.0276)
                .createdBy(creator)
                .build();
        Shop savedShop = shopRepository.save(shop);

        // when
        Optional<Shop> found = shopRepository.findByIdWithCreator(savedShop.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("가챠샵");
        assertThat(found.get().getCreatedBy().getNickname()).isEqualTo("제보자");
    }

    @Test
    @DisplayName("반경 내 샵 조회 - 해당 범위 내 샵 반환")
    void findNearbyShops() {
        // given
        Shop nearbyShop = Shop.builder()
                .name("근처샵")
                .addressName("서울시 강남구")
                .latitude(37.4980)
                .longitude(127.0277)
                .createdBy(creator)
                .build();
        shopRepository.save(nearbyShop);

        Shop farShop = Shop.builder()
                .name("먼샵")
                .addressName("부산시 해운대구")
                .latitude(35.1628)
                .longitude(129.1635)
                .createdBy(creator)
                .build();
        shopRepository.save(farShop);

        // when - 강남역 근처 1km 반경 검색
        List<Shop> nearbyShops = shopRepository.findNearbyShops(37.4979, 127.0276, 1.0);

        // then
        assertThat(nearbyShops).hasSize(1);
        assertThat(nearbyShops.get(0).getName()).isEqualTo("근처샵");
    }

    @Test
    @DisplayName("반경 내 샵 조회 - 범위 밖이면 빈 리스트 반환")
    void findNearbyShops_Empty() {
        // given
        Shop farShop = Shop.builder()
                .name("먼샵")
                .addressName("부산시 해운대구")
                .latitude(35.1628)
                .longitude(129.1635)
                .createdBy(creator)
                .build();
        shopRepository.save(farShop);

        // when - 강남역 근처 1km 반경 검색
        List<Shop> nearbyShops = shopRepository.findNearbyShops(37.4979, 127.0276, 1.0);

        // then
        assertThat(nearbyShops).isEmpty();
    }

    @Test
    @DisplayName("반경 0으로 조회 시 빈 리스트 반환")
    void findNearbyShops_ZeroRadius() {
        // given
        Shop shop = Shop.builder()
                .name("가챠샵")
                .addressName("서울시 강남구")
                .latitude(37.4979)
                .longitude(127.0276)
                .createdBy(creator)
                .build();
        shopRepository.save(shop);

        // when - 반경 0km로 검색
        List<Shop> nearbyShops = shopRepository.findNearbyShops(37.4979, 127.0276, 0.0);

        // then
        assertThat(nearbyShops).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 빈 Optional 반환")
    void findByIdWithCreator_NotFound() {
        // when
        Optional<Shop> found = shopRepository.findByIdWithCreator(999999L);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("반경 내 여러 샵 조회")
    void findNearbyShops_MultipleShops() {
        // given - 반경 내 3개의 샵 생성
        Shop shop1 = Shop.builder()
                .name("가챠샵1")
                .addressName("서울시 강남구")
                .latitude(37.4980)
                .longitude(127.0277)
                .createdBy(creator)
                .build();
        Shop shop2 = Shop.builder()
                .name("가챠샵2")
                .addressName("서울시 강남구")
                .latitude(37.4981)
                .longitude(127.0278)
                .createdBy(creator)
                .build();
        Shop shop3 = Shop.builder()
                .name("가챠샵3")
                .addressName("서울시 강남구")
                .latitude(37.4982)
                .longitude(127.0279)
                .createdBy(creator)
                .build();
        shopRepository.saveAll(List.of(shop1, shop2, shop3));

        // when - 1km 반경 검색
        List<Shop> nearbyShops = shopRepository.findNearbyShops(37.4979, 127.0276, 1.0);

        // then
        assertThat(nearbyShops).hasSize(3);
    }

    @Test
    @DisplayName("샵 데이터 없을 때 반경 조회")
    void findNearbyShops_NoShopsExist() {
        // when
        List<Shop> nearbyShops = shopRepository.findNearbyShops(37.4979, 127.0276, 10.0);

        // then
        assertThat(nearbyShops).isEmpty();
    }

    @Nested
    @DisplayName("searchByName - 가게 이름 검색 (pg_trgm)")
    class SearchByName {

        @Test
        @DisplayName("키워드가 포함된 가게 반환")
        void searchByName_returnsMatchingShops() {
            // given
            shopRepository.save(Shop.builder()
                    .name("가챠샵 신사점").addressName("서울시 강남구").latitude(37.4979).longitude(127.0276).createdBy(creator).build());
            shopRepository.save(Shop.builder()
                    .name("가챠월드 홍대점").addressName("서울시 마포구").latitude(37.5563).longitude(126.9234).createdBy(creator).build());
            shopRepository.save(Shop.builder()
                    .name("피규어나라").addressName("서울시 중구").latitude(37.5636).longitude(126.9975).createdBy(creator).build());

            // when
            Page<Shop> result = shopRepository.searchByName("가챠", PageRequest.of(0, 20));

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).extracting(Shop::getName)
                    .containsExactlyInAnyOrder("가챠샵 신사점", "가챠월드 홍대점");
        }

        @Test
        @DisplayName("키워드에 해당하는 가게 없으면 빈 페이지 반환")
        void searchByName_noMatch_returnsEmpty() {
            // given
            shopRepository.save(Shop.builder()
                    .name("가챠샵 신사점").addressName("서울시 강남구").latitude(37.4979).longitude(127.0276).createdBy(creator).build());

            // when
            Page<Shop> result = shopRepository.searchByName("피규어", PageRequest.of(0, 20));

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("대소문자 구분 없이 검색 (ILIKE)")
        void searchByName_caseInsensitive() {
            // given
            shopRepository.save(Shop.builder()
                    .name("GOTCHA Shop").addressName("서울시 강남구").latitude(37.4979).longitude(127.0276).createdBy(creator).build());

            // when
            Page<Shop> result = shopRepository.searchByName("gotcha", PageRequest.of(0, 20));

            // then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("페이지네이션 적용 확인")
        void searchByName_pagination() {
            // given - 3개 저장, size=2로 조회
            shopRepository.save(Shop.builder().name("가챠샵1").addressName("주소1").latitude(37.1).longitude(127.1).createdBy(creator).build());
            shopRepository.save(Shop.builder().name("가챠샵2").addressName("주소2").latitude(37.2).longitude(127.2).createdBy(creator).build());
            shopRepository.save(Shop.builder().name("가챠샵3").addressName("주소3").latitude(37.3).longitude(127.3).createdBy(creator).build());

            // when
            Page<Shop> page0 = shopRepository.searchByName("가챠", PageRequest.of(0, 2));
            Page<Shop> page1 = shopRepository.searchByName("가챠", PageRequest.of(1, 2));

            // then
            assertThat(page0.getContent()).hasSize(2);
            assertThat(page0.getTotalElements()).isEqualTo(3);
            assertThat(page0.hasNext()).isTrue();
            assertThat(page1.getContent()).hasSize(1);
            assertThat(page1.hasNext()).isFalse();
        }

        @Test
        @DisplayName("주소로 검색 - 주소에 키워드 포함된 가게 반환")
        void searchByName_addressMatch() {
            // given
            shopRepository.save(Shop.builder()
                    .name("울퉁불퉁 가챠샵").addressName("서울시 강남구 신사동").latitude(37.4979).longitude(127.0276).createdBy(creator).build());
            shopRepository.save(Shop.builder()
                    .name("홍대 피규어샵").addressName("서울시 마포구 홍대입구").latitude(37.5563).longitude(126.9234).createdBy(creator).build());

            // when
            Page<Shop> result = shopRepository.searchByName("강남", PageRequest.of(0, 20));

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("울퉁불퉁 가챠샵");
        }

        @Test
        @DisplayName("이름과 주소 둘 다 일치하는 경우 중복 없이 반환")
        void searchByName_bothNameAndAddressMatch_noDuplicate() {
            // given - 이름에도 "강남", 주소에도 "강남" 포함
            shopRepository.save(Shop.builder()
                    .name("강남 가챠샵").addressName("서울시 강남구 역삼동").latitude(37.4979).longitude(127.0276).createdBy(creator).build());

            // when
            Page<Shop> result = shopRepository.searchByName("강남", PageRequest.of(0, 20));

            // then - 중복 없이 1개만 반환
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("가게 없을 때 빈 페이지 반환")
        void searchByName_noShopsExist() {
            // when
            Page<Shop> result = shopRepository.searchByName("가챠", PageRequest.of(0, 20));

            // then
            assertThat(result.getContent()).isEmpty();
        }
    }
}
