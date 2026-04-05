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
import com.gotcha.domain.post.entity.Post;
import com.gotcha.domain.post.entity.PostImage;
import com.gotcha.domain.post.entity.PostType;
import com.gotcha.domain.post.exception.PostException;
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

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostTypeRepository postTypeRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostCommentLikeRepository postCommentLikeRepository;
    private final FileStorageService fileStorageService;
    private final SecurityUtil securityUtil;
    private final UserRepository userRepository;

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

        // 4. Post 생성 및 저장
        Post post = Post.builder()
                .user(user)
                .type(postType)
                .title(request.title())
                .content(request.content())
                .build();
        postRepository.save(post);

        log.info("Post created with ID: {}", post.getId());

        // 5. 이미지 저장
        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            savePostImages(post, request.imageUrls());
            log.info("Saved {} images for post {}", request.imageUrls().size(), post.getId());
        }

        // 6. Response 생성
        List<PostImage> images = postImageRepository.findAllByPostIdOrderByDisplayOrder(post.getId());
        return PostResponse.from(post, images);
    }

    public PostDetailResponse getPostDetail(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostException::notFound);

        Long currentUserId = getCurrentUserIdOrNull();

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

    public PostCursorResponse getPostsByCursor(Long typeId, Long cursor, int size) {
        // size+1 개 조회해서 hasNext 판단
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, size + 1);

        List<Post> posts = (typeId != null)
                ? (cursor != null
                        ? postRepository.findAllByTypeIdAndIdLessThanOrderByIdDesc(typeId, cursor, pageable)
                        : postRepository.findAllByTypeIdOrderByIdDesc(typeId, pageable))
                : (cursor != null
                        ? postRepository.findAllByIdLessThanOrderByIdDesc(cursor, pageable)
                        : postRepository.findAllByOrderByIdDesc(pageable));

        boolean hasNext = posts.size() > size;
        List<Post> pageContent = hasNext ? posts.subList(0, size) : posts;

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
                        commentCountMap.getOrDefault(post.getId(), 0L)
                ))
                .toList();

        return PostCursorResponse.of(content, hasNext);
    }

    public PageResponse<PostListItemResponse> getPosts(Long typeId, Pageable pageable) {
        // 1. 게시글 목록 조회 (카테고리 필터)
        Page<Post> postPage = (typeId != null)
                ? postRepository.findAllByTypeIdOrderByCreatedAtDesc(typeId, pageable)
                : postRepository.findAllByOrderByCreatedAtDesc(pageable);

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
                        commentCountMap.getOrDefault(post.getId(), 0L)
                ))
                .toList();

        return PageResponse.from(postPage, content);
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
