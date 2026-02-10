package com.gotcha.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

import com.gotcha.domain.auth.entity.RefreshToken;
import com.gotcha.domain.auth.repository.RefreshTokenRepository;
import com.gotcha.domain.auth.service.SocialUnlinkService;
import com.gotcha.domain.chat.entity.Chat;
import com.gotcha.domain.chat.entity.ChatRoom;
import com.gotcha.domain.chat.repository.ChatRepository;
import com.gotcha.domain.chat.repository.ChatRoomRepository;
import com.gotcha.domain.comment.entity.Comment;
import com.gotcha.domain.comment.repository.CommentRepository;
import com.gotcha.domain.favorite.entity.Favorite;
import com.gotcha.domain.favorite.repository.FavoriteRepository;
import com.gotcha.domain.file.service.FileStorageService;
import com.gotcha.domain.inquiry.entity.Inquiry;
import com.gotcha.domain.inquiry.repository.InquiryRepository;
import com.gotcha.domain.post.entity.Post;
import com.gotcha.domain.post.entity.PostComment;
import com.gotcha.domain.post.entity.PostType;
import com.gotcha.domain.post.repository.PostCommentRepository;
import com.gotcha.domain.post.repository.PostRepository;
import com.gotcha.domain.review.entity.Review;
import com.gotcha.domain.review.entity.ReviewLike;
import com.gotcha.domain.review.repository.ReviewLikeRepository;
import com.gotcha.domain.review.repository.ReviewRepository;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.entity.ShopReport;
import com.gotcha.domain.shop.repository.ShopReportRepository;
import com.gotcha.domain.shop.repository.ShopRepository;
import com.gotcha.domain.user.dto.WithdrawalRequest;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.entity.UserPermission;
import com.gotcha.domain.user.entity.PermissionType;
import com.gotcha.domain.user.entity.WithdrawalReason;
import com.gotcha.domain.user.exception.UserException;
import com.gotcha.domain.user.repository.UserPermissionRepository;
import com.gotcha.domain.user.repository.UserRepository;
import com.gotcha.domain.user.repository.WithdrawalSurveyRepository;
import com.gotcha._global.util.SecurityUtil;
import com.gotcha.config.TestcontainersConfig;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class UserWithdrawalIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private WithdrawalSurveyRepository withdrawalSurveyRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewLikeRepository reviewLikeRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private ShopReportRepository shopReportRepository;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private SecurityUtil securityUtil;

    @MockBean
    private SocialUnlinkService socialUnlinkService;

    @MockBean
    private FileStorageService fileStorageService;

    private User testUser;
    private User otherUser;
    private Shop testShop;

    @BeforeEach
    void setUp() {
        // 테스트 유저 생성
        testUser = userRepository.save(User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("test-social-id")
                .nickname("테스트유저")
                .email("test@example.com")
                .profileImageUrl("https://example.com/profile.jpg")
                .build());

        // 다른 유저 생성 (채팅방 테스트용)
        otherUser = userRepository.save(User.builder()
                .socialType(SocialType.GOOGLE)
                .socialId("other-social-id")
                .nickname("다른유저")
                .email("other@example.com")
                .build());

        // 테스트 가게 생성
        testShop = shopRepository.save(Shop.builder()
                .name("테스트 가게")
                .addressName("서울시 강남구")
                .latitude(37.5665)
                .longitude(126.9780)
                .createdBy(testUser)
                .build());

        // Mock 설정
        doNothing().when(socialUnlinkService).unlinkSocialAccount(org.mockito.ArgumentMatchers.any());
        doNothing().when(fileStorageService).deleteFile(anyString());
    }

    @Nested
    @DisplayName("회원 탈퇴 통합 테스트")
    class WithdrawIntegration {

        @Test
        @DisplayName("모든 관련 데이터가 있는 상태에서 탈퇴하면 모두 삭제/익명화된다")
        void shouldDeleteAllRelatedDataOnWithdrawal() {
            // given - 모든 관련 데이터 생성
            setupAllUserData();

            org.mockito.Mockito.when(securityUtil.getCurrentUserId()).thenReturn(testUser.getId());

            WithdrawalRequest request = new WithdrawalRequest(
                    List.of(WithdrawalReason.LOW_USAGE),
                    "테스트 탈퇴"
            );

            // when
            userService.withdraw(request);

            // 영속성 컨텍스트 초기화 (DB 상태 확인을 위해)
            entityManager.flush();
            entityManager.clear();

            // then - User soft delete 및 마스킹 확인
            User deletedUser = userRepository.findById(testUser.getId()).orElseThrow();
            assertThat(deletedUser.getIsDeleted()).isTrue();
            assertThat(deletedUser.getNickname()).isEqualTo("탈퇴한 사용자_" + testUser.getId());
            assertThat(deletedUser.getEmail()).isNull();
            assertThat(deletedUser.getProfileImageUrl()).isNull();
            assertThat(deletedUser.getSocialId()).isNull();
            assertThat(deletedUser.getSocialType()).isNull();

            // then - 탈퇴 설문 저장 확인
            assertThat(withdrawalSurveyRepository.findAll()).hasSize(1);

            // then - Hard Delete 확인
            assertThat(favoriteRepository.findAll()).isEmpty();
            assertThat(reviewRepository.findAllByUserId(testUser.getId())).isEmpty();
            assertThat(reviewLikeRepository.findAll()).isEmpty();
            assertThat(commentRepository.findAll()).isEmpty();
            assertThat(userPermissionRepository.findAll()).isEmpty();
            assertThat(refreshTokenRepository.findAll()).isEmpty();
            assertThat(shopReportRepository.findAll()).isEmpty();
            assertThat(inquiryRepository.findAll()).isEmpty();
            assertThat(chatRepository.findAll()).isEmpty();
            assertThat(chatRoomRepository.findAll()).isEmpty();
            assertThat(postCommentRepository.findAll()).isEmpty();
            assertThat(postRepository.findAll()).isEmpty();

            // then - Shop createdBy FK 유지 확인 (soft delete된 사용자 참조 유지)
            Shop updatedShop = shopRepository.findById(testShop.getId()).orElseThrow();
            assertThat(updatedShop.getCreatedBy()).isNotNull();
            assertThat(updatedShop.getCreatedBy().getId()).isEqualTo(testUser.getId());
            assertThat(updatedShop.getName()).isEqualTo("테스트 가게"); // 가게 정보는 유지
        }

        @Test
        @DisplayName("관련 데이터가 없어도 탈퇴가 정상 동작한다")
        void shouldWorkWithNoRelatedData() {
            // given - 데이터 없이 유저만 있는 상태
            org.mockito.Mockito.when(securityUtil.getCurrentUserId()).thenReturn(testUser.getId());

            WithdrawalRequest request = new WithdrawalRequest(
                    List.of(WithdrawalReason.OTHER),
                    null
            );

            // when
            userService.withdraw(request);

            // then
            User deletedUser = userRepository.findById(testUser.getId()).orElseThrow();
            assertThat(deletedUser.getIsDeleted()).isTrue();
            assertThat(withdrawalSurveyRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("이미 탈퇴한 유저가 다시 탈퇴 시도하면 예외가 발생한다")
        void shouldThrowExceptionWhenAlreadyDeleted() {
            // given - 먼저 탈퇴 처리
            org.mockito.Mockito.when(securityUtil.getCurrentUserId()).thenReturn(testUser.getId());

            WithdrawalRequest request = new WithdrawalRequest(
                    List.of(WithdrawalReason.LOW_USAGE),
                    null
            );
            userService.withdraw(request);

            // when & then - 다시 탈퇴 시도
            assertThatThrownBy(() -> userService.withdraw(request))
                    .isInstanceOf(UserException.class)
                    .hasMessageContaining("이미 탈퇴한 사용자");
        }

        @Test
        @DisplayName("다른 유저의 데이터는 삭제되지 않는다")
        void shouldNotDeleteOtherUsersData() {
            // given - 다른 유저의 데이터 생성
            Review otherUserReview = reviewRepository.save(Review.builder()
                    .shop(testShop)
                    .user(otherUser)
                    .content("다른 유저의 리뷰")
                    .build());

            Favorite otherUserFavorite = favoriteRepository.save(Favorite.builder()
                    .user(otherUser)
                    .shop(testShop)
                    .build());

            // 테스트 유저 데이터도 생성
            reviewRepository.save(Review.builder()
                    .shop(testShop)
                    .user(testUser)
                    .content("테스트 유저의 리뷰")
                    .build());

            org.mockito.Mockito.when(securityUtil.getCurrentUserId()).thenReturn(testUser.getId());

            WithdrawalRequest request = new WithdrawalRequest(
                    List.of(WithdrawalReason.LOW_USAGE),
                    null
            );

            // when
            userService.withdraw(request);

            // then - 다른 유저의 데이터는 유지
            assertThat(reviewRepository.findById(otherUserReview.getId())).isPresent();
            assertThat(favoriteRepository.findById(otherUserFavorite.getId())).isPresent();

            // then - 테스트 유저의 데이터는 삭제
            assertThat(reviewRepository.findAllByUserId(testUser.getId())).isEmpty();
        }

        private void setupAllUserData() {
            // Favorite
            favoriteRepository.save(Favorite.builder()
                    .user(testUser)
                    .shop(testShop)
                    .build());

            // Review
            Review review = reviewRepository.save(Review.builder()
                    .shop(testShop)
                    .user(testUser)
                    .content("테스트 리뷰")
                    .build());

            // ReviewLike (내가 누른 좋아요)
            reviewLikeRepository.save(ReviewLike.builder()
                    .review(review)
                    .user(testUser)
                    .build());

            // Comment
            commentRepository.save(Comment.builder()
                    .shop(testShop)
                    .user(testUser)
                    .content("테스트 댓글")
                    .build());

            // UserPermission
            userPermissionRepository.save(UserPermission.builder()
                    .user(testUser)
                    .permissionType(PermissionType.LOCATION)
                    .isAgreed(true)
                    .build());

            // RefreshToken
            refreshTokenRepository.save(RefreshToken.builder()
                    .user(testUser)
                    .token("test-refresh-token")
                    .expiresAt(java.time.LocalDateTime.now().plusDays(7))
                    .build());

            // ShopReport
            shopReportRepository.save(ShopReport.builder()
                    .shop(testShop)
                    .reporter(testUser)
                    .reportTitle("정보 수정")
                    .reportContent("영업시간 변경")
                    .isAnonymous(false)
                    .build());

            // Inquiry
            inquiryRepository.save(Inquiry.builder()
                    .user(testUser)
                    .content("문의 내용")
                    .build());

            // ChatRoom & Chat
            ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.builder()
                    .user1(testUser)
                    .user2(otherUser)
                    .build());

            chatRepository.save(Chat.builder()
                    .chatRoom(chatRoom)
                    .sender(testUser)
                    .content("테스트 메시지")
                    .build());

            // Post & PostComment
            PostType postType = createPostTypeIfNotExists();
            Post post = postRepository.save(Post.builder()
                    .user(testUser)
                    .type(postType)
                    .title("테스트 게시글")
                    .content("게시글 내용")
                    .postImageUrl("https://example.com/post-image.jpg")
                    .build());

            postCommentRepository.save(PostComment.builder()
                    .post(post)
                    .user(testUser)
                    .content("테스트 게시글 댓글")
                    .isAnonymous(false)
                    .build());
        }

        private PostType createPostTypeIfNotExists() {
            PostType postType = PostType.builder()
                    .typeName("자유게시판")
                    .description("자유롭게 글을 작성하는 게시판")
                    .build();
            entityManager.persist(postType);
            entityManager.flush();
            return postType;
        }
    }
}
