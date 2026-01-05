package com.gotcha.domain.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gotcha.config.TestcontainersConfig;
import com.gotcha.domain.comment.entity.Comment;
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
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShopRepository shopRepository;

    private User user;
    private Shop shop;

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
                .address("서울시 강남구")
                .latitude(37.4979)
                .longitude(127.0276)
                .createdBy(creator)
                .build());
    }

    @Test
    @DisplayName("샵 ID로 댓글 페이지 조회")
    void findAllByShopIdOrderByCreatedAtDesc() {
        // given
        commentRepository.save(Comment.builder()
                .shop(shop)
                .user(user)
                .content("첫번째 댓글")
                .isAnonymous(false)
                .build());

        commentRepository.save(Comment.builder()
                .shop(shop)
                .user(user)
                .content("두번째 댓글")
                .isAnonymous(false)
                .build());

        commentRepository.save(Comment.builder()
                .shop(shop)
                .user(user)
                .content("세번째 댓글")
                .isAnonymous(true)
                .build());

        // when
        Page<Comment> commentPage = commentRepository.findAllByShopIdOrderByCreatedAtDesc(
                shop.getId(), PageRequest.of(0, 2));

        // then
        assertThat(commentPage.getContent()).hasSize(2);
        assertThat(commentPage.getTotalElements()).isEqualTo(3);
        assertThat(commentPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("샵 ID로 댓글 조회 - 댓글 없는 경우 빈 페이지 반환")
    void findAllByShopIdOrderByCreatedAtDesc_Empty() {
        // when
        Page<Comment> commentPage = commentRepository.findAllByShopIdOrderByCreatedAtDesc(
                shop.getId(), PageRequest.of(0, 10));

        // then
        assertThat(commentPage.getContent()).isEmpty();
        assertThat(commentPage.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 샵 ID로 댓글 조회 시 빈 페이지 반환")
    void findAllByShopIdOrderByCreatedAtDesc_NonExistentShop() {
        // when
        Page<Comment> commentPage = commentRepository.findAllByShopIdOrderByCreatedAtDesc(
                999999L, PageRequest.of(0, 10));

        // then
        assertThat(commentPage.getContent()).isEmpty();
        assertThat(commentPage.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("페이지 범위 초과 시 빈 페이지 반환")
    void findAllByShopIdOrderByCreatedAtDesc_PageOutOfRange() {
        // given
        commentRepository.save(Comment.builder()
                .shop(shop)
                .user(user)
                .content("댓글")
                .isAnonymous(false)
                .build());

        // when - 페이지 100 요청 (데이터 1개뿐)
        Page<Comment> commentPage = commentRepository.findAllByShopIdOrderByCreatedAtDesc(
                shop.getId(), PageRequest.of(100, 10));

        // then
        assertThat(commentPage.getContent()).isEmpty();
        assertThat(commentPage.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("페이지 크기가 전체 데이터보다 클 때 모든 데이터 반환")
    void findAllByShopIdOrderByCreatedAtDesc_LargePageSize() {
        // given
        commentRepository.save(Comment.builder()
                .shop(shop).user(user).content("댓글1").isAnonymous(false).build());
        commentRepository.save(Comment.builder()
                .shop(shop).user(user).content("댓글2").isAnonymous(false).build());

        // when - 페이지 크기 100으로 요청
        Page<Comment> commentPage = commentRepository.findAllByShopIdOrderByCreatedAtDesc(
                shop.getId(), PageRequest.of(0, 100));

        // then
        assertThat(commentPage.getContent()).hasSize(2);
        assertThat(commentPage.getTotalElements()).isEqualTo(2);
    }
}
