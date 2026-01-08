package com.gotcha.domain.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gotcha.config.TestcontainersConfig;
import com.gotcha.domain.review.entity.Review;
import com.gotcha.domain.review.entity.ReviewImage;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.repository.ShopRepository;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.List;
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
class ReviewImageRepositoryTest {

    @Autowired
    private ReviewImageRepository reviewImageRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShopRepository shopRepository;

    private User user;
    private Shop shop;
    private Review review;

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
                .name("가챠샵")
                .addressName("서울시 강남구")
                .latitude(37.4979)
                .longitude(127.0276)
                .createdBy(creator)
                .build());

        review = reviewRepository.save(Review.builder()
                .shop(shop)
                .user(user)
                .content("테스트 리뷰입니다")
                .build());
    }

    @Test
    @DisplayName("Review ID로 이미지 조회 - displayOrder 순서대로 정렬")
    void findAllByReviewIdOrderByDisplayOrder() {
        // given
        reviewImageRepository.save(ReviewImage.builder()
                .review(review)
                .imageUrl("https://example.com/image2.jpg")
                .displayOrder(2)
                .build());

        reviewImageRepository.save(ReviewImage.builder()
                .review(review)
                .imageUrl("https://example.com/image0.jpg")
                .displayOrder(0)
                .build());

        reviewImageRepository.save(ReviewImage.builder()
                .review(review)
                .imageUrl("https://example.com/image1.jpg")
                .displayOrder(1)
                .build());

        // when
        List<ReviewImage> images = reviewImageRepository
                .findAllByReviewIdOrderByDisplayOrder(review.getId());

        // then
        assertThat(images).hasSize(3);
        assertThat(images.get(0).getDisplayOrder()).isEqualTo(0);
        assertThat(images.get(1).getDisplayOrder()).isEqualTo(1);
        assertThat(images.get(2).getDisplayOrder()).isEqualTo(2);
        assertThat(images.get(0).getImageUrl()).isEqualTo("https://example.com/image0.jpg");
    }

    @Test
    @DisplayName("여러 Review의 이미지 일괄 조회 - N+1 방지")
    void findAllByReviewIdInOrderByReviewIdAscDisplayOrderAsc() {
        // given
        Review review2 = reviewRepository.save(Review.builder()
                .shop(shop)
                .user(user)
                .content("두번째 리뷰")
                .build());

        // review 1에 이미지 2개
        reviewImageRepository.save(ReviewImage.builder()
                .review(review)
                .imageUrl("https://example.com/r1-img0.jpg")
                .displayOrder(0)
                .build());

        reviewImageRepository.save(ReviewImage.builder()
                .review(review)
                .imageUrl("https://example.com/r1-img1.jpg")
                .displayOrder(1)
                .build());

        // review 2에 이미지 2개
        reviewImageRepository.save(ReviewImage.builder()
                .review(review2)
                .imageUrl("https://example.com/r2-img0.jpg")
                .displayOrder(0)
                .build());

        reviewImageRepository.save(ReviewImage.builder()
                .review(review2)
                .imageUrl("https://example.com/r2-img1.jpg")
                .displayOrder(1)
                .build());

        // when
        List<ReviewImage> images = reviewImageRepository
                .findAllByReviewIdInOrderByReviewIdAscDisplayOrderAsc(
                        List.of(review.getId(), review2.getId()));

        // then
        assertThat(images).hasSize(4);
        // reviewId 순서대로, 그 안에서 displayOrder 순서대로 정렬 확인
        assertThat(images.get(0).getReview().getId()).isEqualTo(review.getId());
        assertThat(images.get(0).getDisplayOrder()).isEqualTo(0);
        assertThat(images.get(1).getReview().getId()).isEqualTo(review.getId());
        assertThat(images.get(1).getDisplayOrder()).isEqualTo(1);
        assertThat(images.get(2).getReview().getId()).isEqualTo(review2.getId());
        assertThat(images.get(2).getDisplayOrder()).isEqualTo(0);
        assertThat(images.get(3).getReview().getId()).isEqualTo(review2.getId());
        assertThat(images.get(3).getDisplayOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("Review 삭제 시 연관 이미지 모두 삭제")
    void deleteAllByReviewId() {
        // given
        reviewImageRepository.save(ReviewImage.builder()
                .review(review)
                .imageUrl("https://example.com/image1.jpg")
                .displayOrder(0)
                .build());

        reviewImageRepository.save(ReviewImage.builder()
                .review(review)
                .imageUrl("https://example.com/image2.jpg")
                .displayOrder(1)
                .build());

        reviewImageRepository.save(ReviewImage.builder()
                .review(review)
                .imageUrl("https://example.com/image3.jpg")
                .displayOrder(2)
                .build());

        // when
        reviewImageRepository.deleteAllByReviewId(review.getId());

        // then
        List<ReviewImage> images = reviewImageRepository
                .findAllByReviewIdOrderByDisplayOrder(review.getId());
        assertThat(images).isEmpty();
    }

    @Test
    @DisplayName("이미지 개수 카운트")
    void countByReviewId() {
        // given
        for (int i = 0; i < 5; i++) {
            reviewImageRepository.save(ReviewImage.builder()
                    .review(review)
                    .imageUrl("https://example.com/image" + i + ".jpg")
                    .displayOrder(i)
                    .build());
        }

        // when
        int count = reviewImageRepository.countByReviewId(review.getId());

        // then
        assertThat(count).isEqualTo(5);
    }

    @Test
    @DisplayName("이미지 없는 Review 조회 시 빈 리스트 반환")
    void findAllByReviewIdOrderByDisplayOrder_Empty() {
        // when
        List<ReviewImage> images = reviewImageRepository
                .findAllByReviewIdOrderByDisplayOrder(review.getId());

        // then
        assertThat(images).isEmpty();
    }

    @Test
    @DisplayName("이미지 개수 카운트 - 이미지 없는 경우 0 반환")
    void countByReviewId_Empty() {
        // when
        int count = reviewImageRepository.countByReviewId(review.getId());

        // then
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 Review ID로 조회 시 빈 리스트 반환")
    void findAllByReviewIdOrderByDisplayOrder_NonExistentReview() {
        // when
        List<ReviewImage> images = reviewImageRepository
                .findAllByReviewIdOrderByDisplayOrder(999999L);

        // then
        assertThat(images).isEmpty();
    }
}
