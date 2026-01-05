package com.gotcha.domain.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gotcha.config.TestcontainersConfig;
import com.gotcha.domain.review.entity.Review;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.repository.ShopRepository;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShopRepository shopRepository;

    private User user;
    private User user2;
    private Shop shop;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("user123")
                .nickname("테스트유저")
                .build());

        user2 = userRepository.save(User.builder()
                .socialType(SocialType.NAVER)
                .socialId("user456")
                .nickname("다른유저")
                .build());

        User creator = userRepository.save(User.builder()
                .socialType(SocialType.GOOGLE)
                .socialId("creator123")
                .nickname("제보자")
                .build());

        shop = shopRepository.save(Shop.builder()
                .name("가챠샵")
                .address("서울시 강남구")
                .latitude(37.4979)
                .longitude(127.0276)
                .createdBy(creator)
                .build());
    }

    @Test
    @DisplayName("샵 ID로 리뷰 페이지 조회")
    void findAllByShopIdOrderByCreatedAtDesc() {
        // given
        reviewRepository.save(Review.builder()
                .shop(shop)
                .user(user)
                .content("첫번째 리뷰")
                .build());

        reviewRepository.save(Review.builder()
                .shop(shop)
                .user(user2)
                .content("두번째 리뷰")
                .imageUrl("https://example.com/image.jpg")
                .build());

        // when
        Page<Review> reviewPage = reviewRepository.findAllByShopIdOrderByCreatedAtDesc(
                shop.getId(), PageRequest.of(0, 10));

        // then
        assertThat(reviewPage.getContent()).hasSize(2);
        assertThat(reviewPage.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("사용자가 해당 샵에 리뷰 작성 여부 확인 - 존재하는 경우")
    void existsByUserIdAndShopId_True() {
        // given
        reviewRepository.save(Review.builder()
                .shop(shop)
                .user(user)
                .content("리뷰 내용")
                .build());

        // when
        boolean exists = reviewRepository.existsByUserIdAndShopId(user.getId(), shop.getId());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("사용자가 해당 샵에 리뷰 작성 여부 확인 - 존재하지 않는 경우")
    void existsByUserIdAndShopId_False() {
        // when
        boolean exists = reviewRepository.existsByUserIdAndShopId(user.getId(), shop.getId());

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("샵 ID로 리뷰 조회 - 리뷰 없는 경우 빈 페이지 반환")
    void findAllByShopIdOrderByCreatedAtDesc_Empty() {
        // when
        Page<Review> reviewPage = reviewRepository.findAllByShopIdOrderByCreatedAtDesc(
                shop.getId(), PageRequest.of(0, 10));

        // then
        assertThat(reviewPage.getContent()).isEmpty();
        assertThat(reviewPage.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 샵 ID로 리뷰 조회 시 빈 페이지 반환")
    void findAllByShopIdOrderByCreatedAtDesc_NonExistentShop() {
        // when
        Page<Review> reviewPage = reviewRepository.findAllByShopIdOrderByCreatedAtDesc(
                999999L, PageRequest.of(0, 10));

        // then
        assertThat(reviewPage.getContent()).isEmpty();
        assertThat(reviewPage.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 사용자/샵으로 리뷰 존재 여부 확인 시 false 반환")
    void existsByUserIdAndShopId_NonExistentUserOrShop() {
        // when
        boolean exists1 = reviewRepository.existsByUserIdAndShopId(999999L, shop.getId());
        boolean exists2 = reviewRepository.existsByUserIdAndShopId(user.getId(), 999999L);

        // then
        assertThat(exists1).isFalse();
        assertThat(exists2).isFalse();
    }

    @Test
    @DisplayName("같은 사용자가 다른 샵에 리뷰 작성 가능")
    void existsByUserIdAndShopId_SameUserDifferentShops() {
        // given
        User shopCreator = userRepository.save(User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("creator456")
                .nickname("다른제보자")
                .build());

        Shop anotherShop = shopRepository.save(Shop.builder()
                .name("다른가챠샵")
                .address("서울시 서초구")
                .latitude(37.4837)
                .longitude(127.0324)
                .createdBy(shopCreator)
                .build());

        reviewRepository.save(Review.builder()
                .shop(shop)
                .user(user)
                .content("첫번째 샵 리뷰")
                .build());

        reviewRepository.save(Review.builder()
                .shop(anotherShop)
                .user(user)
                .content("두번째 샵 리뷰")
                .build());

        // when
        boolean existsInShop1 = reviewRepository.existsByUserIdAndShopId(user.getId(), shop.getId());
        boolean existsInShop2 = reviewRepository.existsByUserIdAndShopId(user.getId(), anotherShop.getId());

        // then
        assertThat(existsInShop1).isTrue();
        assertThat(existsInShop2).isTrue();
    }

    @Test
    @DisplayName("페이지 범위 초과 시 빈 페이지 반환")
    void findAllByShopIdOrderByCreatedAtDesc_PageOutOfRange() {
        // given
        reviewRepository.save(Review.builder()
                .shop(shop)
                .user(user)
                .content("리뷰")
                .build());

        // when
        Page<Review> reviewPage = reviewRepository.findAllByShopIdOrderByCreatedAtDesc(
                shop.getId(), PageRequest.of(100, 10));

        // then
        assertThat(reviewPage.getContent()).isEmpty();
        assertThat(reviewPage.getTotalElements()).isEqualTo(1);
    }
}
