package com.gotcha.domain.post.service;

import com.gotcha._global.common.PageResponse;
import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.file.service.FileStorageService;
import com.gotcha.domain.post.dto.CreatePostRequest;
import com.gotcha.domain.post.dto.PostCommentDetailResponse;
import com.gotcha.domain.post.dto.PostCursorResponse;
import com.gotcha.domain.post.dto.PostDetailResponse;
import com.gotcha.domain.post.dto.PostListItemResponse;
import com.gotcha.domain.post.dto.PostResponse;
import com.gotcha.domain.post.dto.PostShopInfo;
import com.gotcha.domain.post.dto.UpdatePostRequest;
import java.time.LocalDateTime;
import com.gotcha.domain.post.entity.Post;
import com.gotcha.domain.post.entity.PostImage;
import com.gotcha.domain.post.entity.PostType;
import com.gotcha.domain.post.exception.PostException;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.exception.ShopException;
import com.gotcha.domain.shop.repository.ShopRepository;
import com.gotcha.domain.post.repository.PostCommentLikeRepository;
import com.gotcha.domain.post.repository.PostCommentRepository;
import com.gotcha.domain.post.repository.PostImageRepository;
import com.gotcha.domain.post.repository.PostLikeRepository;
import com.gotcha.domain.post.repository.PostRepository;
import com.gotcha.domain.post.repository.PostTypeRepository;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.entity.UserType;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private static final int MAX_PAGE_SIZE = 500;
    private static final int POPULAR_PERIOD_DAYS = 7;

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostTypeRepository postTypeRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostCommentLikeRepository postCommentLikeRepository;
    private final FileStorageService fileStorageService;
    private final SecurityUtil securityUtil;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;

    private static final int MAX_IMAGES = 5;

    @Transactional
    public PostResponse createPost(Long userId, CreatePostRequest request) {
        log.info("Creating post by user {}", userId);

        // 1. 이미지 개수 검증
        if (request.imageUrls() != null && request.imageUrls().size() > MAX_IMAGES) {
            throw PostException.tooManyImages();
        }

        // 2. User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 3. PostType 조회
        PostType postType = postTypeRepository.findById(request.typeId())
                .orElseThrow(PostException::typeNotFound);

        // 4. Shop 조회 (선택)
        Shop shop = null;
        if (request.shopId() != null) {
            shop = shopRepository.findById(request.shopId())
                    .orElseThrow(() -> ShopException.notFound(request.shopId()));
        }

        // 5. Post 생성 및 저장
        Post post = Post.builder()
                .user(user)
                .type(postType)
                .shop(shop)
                .content(request.content())
                .isPublic(request.isPublic())
                .build();
        postRepository.save(post);

        log.info("Post created with ID: {}", post.getId());

        // 6. 이미지 저장
        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            savePostImages(post, request.imageUrls());
            log.info("Saved {} images for post {}", request.imageUrls().size(), post.getId());
        }

        // 7. Response 생성
        List<PostImage> images = postImageRepository.findAllByPostIdOrderByDisplayOrder(post.getId());
        return PostResponse.from(post, images);
    }

    public PostDetailResponse getPostDetail(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostException::notFound);

        Long currentUserId = getCurrentUserIdOrNull();

        // 비공개 게시글: 작성자 본인 또는 ADMIN만 조회 가능
        if (!post.isPublic() && !canAccessPrivate(post, currentUserId)) {
            throw PostException.privatePost();
        }

        // 1. 이미지
        List<PostImage> images = postImageRepository.findAllByPostIdOrderByDisplayOrder(postId);

        // 2. 게시글 좋아요 수 + 여부
        long postLikeCount = postLikeRepository.countByPostId(postId);
        boolean isPostLiked = currentUserId != null &&
                postLikeRepository.findByUserIdAndPostId(currentUserId, postId).isPresent();
        boolean isOwner = currentUserId != null && post.getUser().getId().equals(currentUserId);

        // 3. 최상위 댓글 조회
        List<com.gotcha.domain.post.entity.PostComment> topComments =
                postCommentRepository.findAllByPostIdAndParentIsNullOrderByCreatedAtAsc(postId);

        if (topComments.isEmpty()) {
            return PostDetailResponse.of(post, images, postLikeCount, isPostLiked, isOwner, List.of());
        }

        List<Long> topCommentIds = topComments.stream()
                .map(com.gotcha.domain.post.entity.PostComment::getId).toList();

        // 4. 대댓글 일괄 조회
        List<com.gotcha.domain.post.entity.PostComment> allReplies =
                postCommentRepository.findAllByParentIdInOrderByCreatedAtAsc(topCommentIds);

        Map<Long, List<com.gotcha.domain.post.entity.PostComment>> repliesByParentId = allReplies.stream()
                .collect(Collectors.groupingBy(r -> r.getParent().getId()));

        // 5. 댓글 + 대댓글 전체 ID
        List<Long> allCommentIds = new java.util.ArrayList<>(topCommentIds);
        allReplies.stream().map(com.gotcha.domain.post.entity.PostComment::getId).forEach(allCommentIds::add);

        // 6. 댓글 좋아요 수 일괄 조회
        Map<Long, Long> commentLikeCountMap = postCommentLikeRepository
                .countByPostCommentIdIn(allCommentIds).stream()
                .collect(Collectors.toMap(
                        PostCommentLikeRepository.PostCommentLikeCount::getCommentId,
                        PostCommentLikeRepository.PostCommentLikeCount::getLikeCount
                ));

        // 7. 댓글 좋아요 여부 일괄 조회
        Set<Long> likedCommentIds = currentUserId != null
                ? postCommentLikeRepository.findLikedCommentIdsByUserIdAndCommentIdIn(currentUserId, allCommentIds)
                : Collections.emptySet();

        // 8. 응답 조립
        List<PostCommentDetailResponse> commentResponses = topComments.stream()
                .map(comment -> {
                    List<com.gotcha.domain.post.entity.PostComment> replies =
                            repliesByParentId.getOrDefault(comment.getId(), List.of());
                    List<PostCommentDetailResponse> replyResponses = replies.stream()
                            .map(reply -> PostCommentDetailResponse.of(
                                    reply,
                                    currentUserId,
                                    commentLikeCountMap.getOrDefault(reply.getId(), 0L),
                                    likedCommentIds.contains(reply.getId()),
                                    List.of()
                            ))
                            .toList();
                    return PostCommentDetailResponse.of(
                            comment,
                            currentUserId,
                            commentLikeCountMap.getOrDefault(comment.getId(), 0L),
                            likedCommentIds.contains(comment.getId()),
                            replyResponses
                    );
                })
                .toList();

        return PostDetailResponse.of(post, images, postLikeCount, isPostLiked, isOwner, commentResponses);
    }

    public PostCursorResponse getPopularPosts(Long typeId, int page, int size) {
        int effectiveSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        int effectivePage = Math.max(0, page);
        Pageable pageable = org.springframework.data.domain.PageRequest.of(effectivePage, effectiveSize);

        Long currentUserId = getCurrentUserIdOrNull();
        boolean isAdmin = isCurrentUserAdmin(currentUserId);
        LocalDateTime since = LocalDateTime.now().minusDays(POPULAR_PERIOD_DAYS);

        Page<Post> postPage = postRepository.findPopularPosts(typeId, currentUserId, isAdmin, since, pageable);
        List<Post> pageContent = postPage.getContent();

        if (pageContent.isEmpty()) {
            return PostCursorResponse.of(List.of(), false);
        }

        List<Long> postIds = pageContent.stream().map(Post::getId).toList();

        Map<Long, List<PostImage>> imageMap = postImageRepository
                .findAllByPostIdInOrderByDisplayOrder(postIds)
                .stream()
                .collect(Collectors.groupingBy(img -> img.getPost().getId()));

        Map<Long, Long> likeCountMap = postLikeRepository.countByPostIdIn(postIds)
                .stream()
                .collect(Collectors.toMap(
                        PostLikeRepository.PostLikeCount::getPostId,
                        PostLikeRepository.PostLikeCount::getLikeCount
                ));

        Map<Long, Long> commentCountMap = postCommentRepository.countByPostIdIn(postIds)
                .stream()
                .collect(Collectors.toMap(
                        PostCommentRepository.PostCommentCount::getPostId,
                        PostCommentRepository.PostCommentCount::getCommentCount
                ));

        List<PostListItemResponse> content = pageContent.stream()
                .map(post -> PostListItemResponse.of(
                        post,
                        imageMap.getOrDefault(post.getId(), List.of()),
                        likeCountMap.getOrDefault(post.getId(), 0L),
                        commentCountMap.getOrDefault(post.getId(), 0L),
                        currentUserId
                ))
                .toList();

        return PostCursorResponse.of(content, postPage.hasNext());
    }

    public PostCursorResponse getPostsByCursor(Long typeId, Long cursor, int size) {
        int effectiveSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, effectiveSize + 1);

        Long currentUserId = getCurrentUserIdOrNull();
        boolean isAdmin = isCurrentUserAdmin(currentUserId);
        List<Post> posts = postRepository.findVisibleByCursor(typeId, cursor, currentUserId, isAdmin, pageable);

        boolean hasNext = posts.size() > effectiveSize;
        List<Post> pageContent = hasNext ? posts.subList(0, effectiveSize) : posts;

        if (pageContent.isEmpty()) {
            return PostCursorResponse.of(List.of(), false);
        }

        List<Long> postIds = pageContent.stream().map(Post::getId).toList();

        Map<Long, List<PostImage>> imageMap = postImageRepository
                .findAllByPostIdInOrderByDisplayOrder(postIds)
                .stream()
                .collect(Collectors.groupingBy(img -> img.getPost().getId()));

        Map<Long, Long> likeCountMap = postLikeRepository.countByPostIdIn(postIds)
                .stream()
                .collect(Collectors.toMap(
                        PostLikeRepository.PostLikeCount::getPostId,
                        PostLikeRepository.PostLikeCount::getLikeCount
                ));

        Map<Long, Long> commentCountMap = postCommentRepository.countByPostIdIn(postIds)
                .stream()
                .collect(Collectors.toMap(
                        PostCommentRepository.PostCommentCount::getPostId,
                        PostCommentRepository.PostCommentCount::getCommentCount
                ));

        List<PostListItemResponse> content = pageContent.stream()
                .map(post -> PostListItemResponse.of(
                        post,
                        imageMap.getOrDefault(post.getId(), List.of()),
                        likeCountMap.getOrDefault(post.getId(), 0L),
                        commentCountMap.getOrDefault(post.getId(), 0L),
                        currentUserId
                ))
                .toList();

        return PostCursorResponse.of(content, hasNext);
    }

    public PageResponse<PostListItemResponse> getPosts(Long typeId, Pageable pageable) {
        Long currentUserId = getCurrentUserIdOrNull();
        boolean isAdmin = isCurrentUserAdmin(currentUserId);
        // 1. 게시글 목록 조회 (카테고리 필터 + 비공개 가시성 필터)
        Page<Post> postPage = (typeId != null)
                ? postRepository.findVisibleByTypeId(typeId, currentUserId, isAdmin, pageable)
                : postRepository.findVisibleAll(currentUserId, isAdmin, pageable);

        List<Long> postIds = postPage.getContent().stream().map(Post::getId).toList();

        if (postIds.isEmpty()) {
            return PageResponse.from(postPage, List.of());
        }

        // 2. N+1 방지: 이미지 일괄 조회
        Map<Long, List<PostImage>> imageMap = postImageRepository
                .findAllByPostIdInOrderByDisplayOrder(postIds)
                .stream()
                .collect(Collectors.groupingBy(img -> img.getPost().getId()));

        // 3. N+1 방지: 좋아요 수 일괄 조회
        Map<Long, Long> likeCountMap = postLikeRepository.countByPostIdIn(postIds)
                .stream()
                .collect(Collectors.toMap(
                        PostLikeRepository.PostLikeCount::getPostId,
                        PostLikeRepository.PostLikeCount::getLikeCount
                ));

        // 4. N+1 방지: 댓글 수 일괄 조회
        Map<Long, Long> commentCountMap = postCommentRepository.countByPostIdIn(postIds)
                .stream()
                .collect(Collectors.toMap(
                        PostCommentRepository.PostCommentCount::getPostId,
                        PostCommentRepository.PostCommentCount::getCommentCount
                ));

        List<PostListItemResponse> content = postPage.getContent().stream()
                .map(post -> PostListItemResponse.of(
                        post,
                        imageMap.getOrDefault(post.getId(), List.of()),
                        likeCountMap.getOrDefault(post.getId(), 0L),
                        commentCountMap.getOrDefault(post.getId(), 0L),
                        currentUserId
                ))
                .toList();

        return PageResponse.from(postPage, content);
    }

    @Transactional
    public PostResponse updatePost(Long postId, UpdatePostRequest request) {
        // 1. 이미지 개수 검증
        if (request.imageUrls() != null && request.imageUrls().size() > MAX_IMAGES) {
            throw PostException.tooManyImages();
        }

        // 2. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(PostException::notFound);

        // 3. 본인 확인 (ADMIN은 수정 불가, 작성자 본인만 가능)
        User currentUser = securityUtil.getCurrentUser();
        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw PostException.unauthorized();
        }

        // 4. PostType 조회
        PostType postType = postTypeRepository.findById(request.typeId())
                .orElseThrow(PostException::typeNotFound);

        // 5. Shop 조회 (선택, null이면 매장 연결 해제)
        Shop shop = null;
        if (request.shopId() != null) {
            shop = shopRepository.findById(request.shopId())
                    .orElseThrow(() -> ShopException.notFound(request.shopId()));
        }

        // 6. 게시글 본문/카테고리/매장 갱신
        post.update(postType, shop, request.content());

        // 7. 이미지 교체: 기존 이미지 S3 + DB 삭제 후 신규 저장
        List<PostImage> oldImages = postImageRepository.findAllByPostIdOrderByDisplayOrder(postId);
        for (PostImage image : oldImages) {
            try {
                fileStorageService.deleteFile(image.getImageUrl());
            } catch (Exception e) {
                log.warn("Failed to delete post image from S3: {}", image.getImageUrl());
            }
        }
        postImageRepository.deleteAllByPostId(postId);

        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            savePostImages(post, request.imageUrls());
        }

        log.info("Post updated - postId: {}, userId: {}", postId, currentUser.getId());

        List<PostImage> newImages = postImageRepository.findAllByPostIdOrderByDisplayOrder(postId);
        return PostResponse.from(post, newImages);
    }

    @Transactional
    public void deletePost(Long postId) {
        User currentUser = securityUtil.getCurrentUser();

        Post post = postRepository.findById(postId)
                .orElseThrow(PostException::notFound);

        boolean isAdmin = currentUser.getUserType() == UserType.ADMIN;
        if (!isAdmin && !post.getUser().getId().equals(currentUser.getId())) {
            throw PostException.unauthorized();
        }

        // 1. 이미지 S3 삭제 + DB 삭제
        List<PostImage> images = postImageRepository.findAllByPostIdOrderByDisplayOrder(postId);
        for (PostImage image : images) {
            try {
                fileStorageService.deleteFile(image.getImageUrl());
            } catch (Exception e) {
                log.warn("Failed to delete post image from S3: {}", image.getImageUrl());
            }
        }
        postImageRepository.deleteAllByPostId(postId);

        // 2. 댓글 좋아요 삭제
        postCommentLikeRepository.deleteAllByPostIdIn(List.of(postId));

        // 3. 댓글/대댓글 삭제
        postCommentRepository.deleteByPostIdIn(List.of(postId));

        // 4. 게시글 좋아요 삭제
        postLikeRepository.deleteAllByPostId(postId);

        // 5. 게시글 삭제
        postRepository.delete(post);

        log.info("Post deleted - postId: {}, deletedBy: {}", postId, currentUser.getId());
    }

    private Long getCurrentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            return null;
        }
        return (Long) auth.getPrincipal();
    }

    private boolean canAccessPrivate(Post post, Long currentUserId) {
        if (currentUserId == null) {
            return false;
        }
        if (post.getUser().getId().equals(currentUserId)) {
            return true;
        }
        return isCurrentUserAdmin(currentUserId);
    }

    private boolean isCurrentUserAdmin(Long currentUserId) {
        if (currentUserId == null) {
            return false;
        }
        return userRepository.findById(currentUserId)
                .map(u -> u.getUserType() == UserType.ADMIN)
                .orElse(false);
    }

    public List<PostShopInfo> searchShopsForPost(String keyword) {
        if (keyword == null || keyword.trim().length() < 2) {
            return List.of();
        }
        List<Shop> shops = shopRepository.searchByName(
                keyword.trim(),
                org.springframework.data.domain.PageRequest.of(0, 50)
        );
        return shops.stream().map(PostShopInfo::from).toList();
    }

    private void savePostImages(Post post, List<String> imageUrls) {
        for (int i = 0; i < imageUrls.size(); i++) {
            postImageRepository.save(PostImage.builder()
                    .post(post)
                    .imageUrl(imageUrls.get(i))
                    .displayOrder(i)
                    .build());
        }
    }
}
