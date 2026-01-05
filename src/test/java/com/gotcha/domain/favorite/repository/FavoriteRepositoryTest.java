package com.gotcha.domain.favorite.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gotcha.config.TestcontainersConfig;
import com.gotcha.domain.favorite.entity.Favorite;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.repository.ShopRepository;
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
class FavoriteRepositoryTest {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShopRepository shopRepository;

    private User user;
    private Shop shop;
    private Shop shop2;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("user123")
                .nickname("테스트유저")
                .build());

        User creator = userRepository.save(User.builder()
                .socialType(SocialType.GOOGLE)
                .socialId("creator123")
                .nickname("제보자")
                .build());

        shop = shopRepository.save(Shop.builder()
                .name("가챠샵1")
                .address("서울시 강남구")
                .latitude(37.4979)
                .longitude(127.0276)
                .createdBy(creator)
                .build());

        shop2 = shopRepository.save(Shop.builder()
                .name("가챠샵2")
                .address("서울시 서초구")
                .latitude(37.4837)
                .longitude(127.0324)
                .createdBy(creator)
                .build());
    }

    @Test
    @DisplayName("사용자 ID와 샵 ID로 즐겨찾기 조회")
    void findByUserIdAndShopId() {
        // given
        Favorite favorite = Favorite.builder()
                .user(user)
                .shop(shop)
                .build();
        favoriteRepository.save(favorite);

        // when
        Optional<Favorite> found = favoriteRepository.findByUserIdAndShopId(user.getId(), shop.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
        assertThat(found.get().getShop().getId()).isEqualTo(shop.getId());
    }

    @Test
    @DisplayName("사용자의 모든 즐겨찾기 조회")
    void findAllByUserId() {
        // given
        favoriteRepository.save(Favorite.builder().user(user).shop(shop).build());
        favoriteRepository.save(Favorite.builder().user(user).shop(shop2).build());

        // when
        List<Favorite> favorites = favoriteRepository.findAllByUserId(user.getId());

        // then
        assertThat(favorites).hasSize(2);
    }

    @Test
    @DisplayName("샵의 즐겨찾기 수 조회")
    void countByShopId() {
        // given
        User user2 = userRepository.save(User.builder()
                .socialType(SocialType.NAVER)
                .socialId("user456")
                .nickname("다른유저")
                .build());

        favoriteRepository.save(Favorite.builder().user(user).shop(shop).build());
        favoriteRepository.save(Favorite.builder().user(user2).shop(shop).build());

        // when
        Long count = favoriteRepository.countByShopId(shop.getId());

        // then
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("사용자 ID와 샵 ID로 즐겨찾기 삭제")
    void deleteByUserIdAndShopId() {
        // given
        Favorite favorite = Favorite.builder()
                .user(user)
                .shop(shop)
                .build();
        favoriteRepository.save(favorite);

        // when
        favoriteRepository.deleteByUserIdAndShopId(user.getId(), shop.getId());
        favoriteRepository.flush();

        // then
        Optional<Favorite> found = favoriteRepository.findByUserIdAndShopId(user.getId(), shop.getId());
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 즐겨찾기 조회 시 빈 Optional 반환")
    void findByUserIdAndShopId_NotFound() {
        // when
        Optional<Favorite> found = favoriteRepository.findByUserIdAndShopId(user.getId(), shop.getId());

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("즐겨찾기 없는 사용자의 목록 조회 시 빈 리스트 반환")
    void findAllByUserId_Empty() {
        // when
        List<Favorite> favorites = favoriteRepository.findAllByUserId(user.getId());

        // then
        assertThat(favorites).isEmpty();
    }

    @Test
    @DisplayName("즐겨찾기 없는 샵의 카운트 조회 시 0 반환")
    void countByShopId_Zero() {
        // when
        Long count = favoriteRepository.countByShopId(shop.getId());

        // then
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 즐겨찾기 삭제 시 에러 없이 진행")
    void deleteByUserIdAndShopId_NotExists() {
        // when & then - 존재하지 않는 데이터 삭제 시 에러 없음
        favoriteRepository.deleteByUserIdAndShopId(user.getId(), shop.getId());
        favoriteRepository.flush();

        // 검증 - 에러 없이 정상 완료
        assertThat(favoriteRepository.findByUserIdAndShopId(user.getId(), shop.getId())).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 조회 시 빈 리스트 반환")
    void findAllByUserId_NonExistentUser() {
        // when
        List<Favorite> favorites = favoriteRepository.findAllByUserId(999999L);

        // then
        assertThat(favorites).isEmpty();
    }
}
