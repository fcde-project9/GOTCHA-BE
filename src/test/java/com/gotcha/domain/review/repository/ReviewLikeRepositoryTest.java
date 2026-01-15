package com.gotcha.domain.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gotcha.config.TestcontainersConfig;
import com.gotcha.domain.review.entity.Review;
import com.gotcha.domain.review.entity.ReviewLike;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.repository.ShopRepository;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
class ReviewLikeRepositoryTest {

    @Autowired
    private ReviewLikeRepository reviewLikeRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShopRepository shopRepository;

    private User user1;
    private User user2;
    private User user3;
    private Shop shop;
    private Review review1;
    private Review review2;
    private Review review3;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("user1")
                .nickname("유저1")
                .build());

        user2 = userRepository.save(User.builder()
                .socialType(SocialType.GOOGLE)
                .socialId("user2")
                .nickname("유저2")
                .build());

        user3 = userRepository.save(User.builder()
                .socialType(SocialType.NAVER)
                .socialId("user3")
                .nickname("유저3")
                .build());

        shop = shopRepository.save(Shop.builder()
                .name("가챠샵")
                .addressName("서울시 강남구")
                .latitude(37.4979)
                .longitude(127.0276)
                .createdBy(user1)
                .build());

        review1 = reviewRepository.save(Review.builder()
                .shop(shop)
                .user(user1)
                .content("리뷰1")
                .build());

        review2 = reviewRepository.save(Review.builder()
                .shop(shop)
                .user(user2)
                .content("리뷰2")
                .build());

        review3 = reviewRepository.save(Review.builder()
                .shop(shop)
                .user(user3)
                .content("리뷰3")
                .build());
    }

    @Test
    @DisplayName("여러 리뷰의 좋아요 수를 일괄 조회 - 정상 케이스")
    void countByReviewIdInGroupByReviewId_Success() {
        // given: review1 = 2개, review2 = 1개, review3 = 0개
        reviewLikeRepository.save(ReviewLike.builder().user(user1).review(review1).build());
        reviewLikeRepository.save(ReviewLike.builder().user(user2).review(review1).build());
        reviewLikeRepository.save(ReviewLike.builder().user(user1).review(review2).build());

        List<Long> reviewIds = List.of(review1.getId(), review2.getId(), review3.getId());

        // when
        List<ReviewLikeRepository.ReviewLikeCount> results =
                reviewLikeRepository.countByReviewIdInGroupByReviewId(reviewIds);

        // then
        Map<Long, Long> countMap = results.stream()
                .collect(Collectors.toMap(
                        ReviewLikeRepository.ReviewLikeCount::getReviewId,
                        ReviewLikeRepository.ReviewLikeCount::getLikeCount
                ));

        assertThat(countMap).hasSize(2); // 좋아요가 있는 리뷰만 반환
        assertThat(countMap.get(review1.getId())).isEqualTo(2L);
        assertThat(countMap.get(review2.getId())).isEqualTo(1L);
        assertThat(countMap.containsKey(review3.getId())).isFalse(); // 좋아요 0개는 결과에 없음
    }

    @Test
    @DisplayName("여러 리뷰의 좋아요 수를 일괄 조회 - 빈 리스트 입력")
    void countByReviewIdInGroupByReviewId_EmptyList() {
        // when
        List<ReviewLikeRepository.ReviewLikeCount> results =
                reviewLikeRepository.countByReviewIdInGroupByReviewId(List.of());

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("여러 리뷰의 좋아요 수를 일괄 조회 - 좋아요가 없는 리뷰들")
    void countByReviewIdInGroupByReviewId_NoLikes() {
        // given
        List<Long> reviewIds = List.of(review1.getId(), review2.getId(), review3.getId());

        // when
        List<ReviewLikeRepository.ReviewLikeCount> results =
                reviewLikeRepository.countByReviewIdInGroupByReviewId(reviewIds);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("특정 사용자가 좋아요한 리뷰 ID 목록 조회 - 정상 케이스")
    void findLikedReviewIds_Success() {
        // given: user1이 review1, review2에 좋아요
        reviewLikeRepository.save(ReviewLike.builder().user(user1).review(review1).build());
        reviewLikeRepository.save(ReviewLike.builder().user(user1).review(review2).build());
        reviewLikeRepository.save(ReviewLike.builder().user(user2).review(review1).build()); // 다른 유저

        List<Long> reviewIds = List.of(review1.getId(), review2.getId(), review3.getId());

        // when
        List<Long> likedReviewIds = reviewLikeRepository.findLikedReviewIds(user1.getId(), reviewIds);

        // then
        assertThat(likedReviewIds).hasSize(2);
        assertThat(likedReviewIds).containsExactlyInAnyOrder(review1.getId(), review2.getId());
    }

    @Test
    @DisplayName("특정 사용자가 좋아요한 리뷰 ID 목록 조회 - 좋아요 없음")
    void findLikedReviewIds_NoLikes() {
        // given: user1이 아무 리뷰에도 좋아요하지 않음
        reviewLikeRepository.save(ReviewLike.builder().user(user2).review(review1).build());

        List<Long> reviewIds = List.of(review1.getId(), review2.getId(), review3.getId());

        // when
        List<Long> likedReviewIds = reviewLikeRepository.findLikedReviewIds(user1.getId(), reviewIds);

        // then
        assertThat(likedReviewIds).isEmpty();
    }

    @Test
    @DisplayName("특정 사용자가 좋아요한 리뷰 ID 목록 조회 - 빈 리스트 입력")
    void findLikedReviewIds_EmptyList() {
        // given: user1이 review1에 좋아요
        reviewLikeRepository.save(ReviewLike.builder().user(user1).review(review1).build());

        // when
        List<Long> likedReviewIds = reviewLikeRepository.findLikedReviewIds(user1.getId(), List.of());

        // then
        assertThat(likedReviewIds).isEmpty();
    }

    @Test
    @DisplayName("특정 사용자가 좋아요한 리뷰 ID 목록 조회 - 존재하지 않는 사용자")
    void findLikedReviewIds_NonExistentUser() {
        // given
        reviewLikeRepository.save(ReviewLike.builder().user(user1).review(review1).build());

        List<Long> reviewIds = List.of(review1.getId(), review2.getId());

        // when
        List<Long> likedReviewIds = reviewLikeRepository.findLikedReviewIds(999999L, reviewIds);

        // then
        assertThat(likedReviewIds).isEmpty();
    }

    @Test
    @DisplayName("좋아요 수 일괄 조회 - 대량 데이터 (페이지네이션 시뮬레이션)")
    void countByReviewIdInGroupByReviewId_LargeData() {
        // given: 20개의 리뷰 생성 (페이지 사이즈 20 시뮬레이션)
        List<Review> reviews = List.of(
                review1, review2, review3,
                createReview("리뷰4"), createReview("리뷰5"), createReview("리뷰6"),
                createReview("리뷰7"), createReview("리뷰8"), createReview("리뷰9"),
                createReview("리뷰10"), createReview("리뷰11"), createReview("리뷰12"),
                createReview("리뷰13"), createReview("리뷰14"), createReview("리뷰15"),
                createReview("리뷰16"), createReview("리뷰17"), createReview("리뷰18"),
                createReview("리뷰19"), createReview("리뷰20")
        );

        // 각 리뷰에 1~3개의 좋아요 추가
        for (int i = 0; i < reviews.size(); i++) {
            Review review = reviews.get(i);
            int likeCount = (i % 3) + 1; // 1, 2, 3, 1, 2, 3, ...
            for (int j = 0; j < likeCount; j++) {
                User liker = (j == 0) ? user1 : (j == 1) ? user2 : user3;
                reviewLikeRepository.save(ReviewLike.builder().user(liker).review(review).build());
            }
        }

        List<Long> reviewIds = reviews.stream().map(Review::getId).toList();

        // when
        List<ReviewLikeRepository.ReviewLikeCount> results =
                reviewLikeRepository.countByReviewIdInGroupByReviewId(reviewIds);

        // then
        assertThat(results).hasSize(20);
        Map<Long, Long> countMap = results.stream()
                .collect(Collectors.toMap(
                        ReviewLikeRepository.ReviewLikeCount::getReviewId,
                        ReviewLikeRepository.ReviewLikeCount::getLikeCount
                ));

        // 각 리뷰의 좋아요 수 검증
        for (int i = 0; i < reviews.size(); i++) {
            int expectedCount = (i % 3) + 1;
            assertThat(countMap.get(reviews.get(i).getId())).isEqualTo((long) expectedCount);
        }
    }

    @Test
    @DisplayName("사용자 좋아요 목록 조회 - 대량 데이터 (페이지네이션 시뮬레이션)")
    void findLikedReviewIds_LargeData() {
        // given: 20개의 리뷰 생성, user1이 짝수 번째 리뷰에만 좋아요
        List<Review> reviews = List.of(
                review1, review2, review3,
                createReview("리뷰4"), createReview("리뷰5"), createReview("리뷰6"),
                createReview("리뷰7"), createReview("리뷰8"), createReview("리뷰9"),
                createReview("리뷰10"), createReview("리뷰11"), createReview("리뷰12"),
                createReview("리뷰13"), createReview("리뷰14"), createReview("리뷰15"),
                createReview("리뷰16"), createReview("리뷰17"), createReview("리뷰18"),
                createReview("리뷰19"), createReview("리뷰20")
        );

        for (int i = 0; i < reviews.size(); i++) {
            if (i % 2 == 0) { // 짝수 인덱스에만 좋아요
                reviewLikeRepository.save(ReviewLike.builder().user(user1).review(reviews.get(i)).build());
            }
        }

        List<Long> reviewIds = reviews.stream().map(Review::getId).toList();

        // when
        List<Long> likedReviewIds = reviewLikeRepository.findLikedReviewIds(user1.getId(), reviewIds);

        // then
        assertThat(likedReviewIds).hasSize(10); // 0, 2, 4, 6, 8, 10, 12, 14, 16, 18

        List<Long> expectedIds = List.of(
                reviews.get(0).getId(), reviews.get(2).getId(), reviews.get(4).getId(),
                reviews.get(6).getId(), reviews.get(8).getId(), reviews.get(10).getId(),
                reviews.get(12).getId(), reviews.get(14).getId(), reviews.get(16).getId(),
                reviews.get(18).getId()
        );
        assertThat(likedReviewIds).containsExactlyInAnyOrderElementsOf(expectedIds);
    }

    private Review createReview(String content) {
        return reviewRepository.save(Review.builder()
                .shop(shop)
                .user(user1)
                .content(content)
                .build());
    }
}
