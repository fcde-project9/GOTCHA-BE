package com.gotcha.domain.post.service;

import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.post.dto.CreatePostCommentRequest;
import com.gotcha.domain.post.dto.PostCommentResponse;
import com.gotcha.domain.post.entity.Post;
import com.gotcha.domain.post.entity.PostComment;
import com.gotcha.domain.post.exception.PostException;
import com.gotcha.domain.post.repository.PostCommentLikeRepository;
import com.gotcha.domain.post.repository.PostCommentRepository;
import com.gotcha.domain.post.repository.PostRepository;
import com.gotcha.domain.user.entity.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostCommentService {

    private final PostCommentRepository postCommentRepository;
    private final PostCommentLikeRepository postCommentLikeRepository;
    private final PostRepository postRepository;
    private final SecurityUtil securityUtil;

    @Transactional
    public PostCommentResponse createComment(Long postId, CreatePostCommentRequest request) {
        User currentUser = securityUtil.getCurrentUser();

        // 1. 게시글 존재 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(PostException::notFound);

        // 2. 부모 댓글 처리 (대댓글인 경우)
        PostComment parent = null;
        if (request.parentId() != null) {
            parent = postCommentRepository.findById(request.parentId())
                    .orElseThrow(PostException::commentNotFound);

            // 대댓글의 대댓글 방지 (depth 1만 허용)
            if (parent.getParent() != null) {
                throw PostException.replyDepthExceeded();
            }

            // 다른 게시글의 댓글에 대댓글 방지
            if (!parent.getPost().getId().equals(postId)) {
                throw PostException.commentNotFound();
            }
        }

        // 3. 댓글 생성 및 저장
        PostComment comment = PostComment.builder()
                .post(post)
                .user(currentUser)
                .parent(parent)
                .content(request.content())
                .isAnonymous(request.isAnonymous())
                .build();
        postCommentRepository.save(comment);

        log.info("Post comment created - postId: {}, userId: {}, parentId: {}",
                postId, currentUser.getId(), request.parentId());

        return PostCommentResponse.from(comment, currentUser.getId());
    }

    @Transactional
    public void deleteComment(Long postId, Long commentId) {
        User currentUser = securityUtil.getCurrentUser();

        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(PostException::commentNotFound);

        if (!comment.getPost().getId().equals(postId)) {
            throw PostException.commentNotFound();
        }

        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw PostException.commentUnauthorized();
        }

        // 최상위 댓글인 경우: 대댓글 + 그 좋아요도 함께 삭제
        if (comment.getParent() == null) {
            List<PostComment> replies = postCommentRepository.findAllByParentId(commentId);
            if (!replies.isEmpty()) {
                List<Long> replyIds = replies.stream().map(PostComment::getId).toList();
                postCommentLikeRepository.deleteAllByPostCommentIdIn(replyIds);
                postCommentRepository.deleteAllByParentId(commentId);
            }
        }

        // 댓글 좋아요 삭제 후 댓글 삭제
        postCommentLikeRepository.deleteAllByPostCommentId(commentId);
        postCommentRepository.delete(comment);

        log.info("Post comment deleted - commentId: {}, userId: {}", commentId, currentUser.getId());
    }
}
