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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

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
}
