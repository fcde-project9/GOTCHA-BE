package com.gotcha.domain.post.service;

import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.post.dto.PostCommentLikeResponse;
import com.gotcha.domain.post.entity.PostComment;
import com.gotcha.domain.post.entity.PostCommentLike;
import com.gotcha.domain.post.exception.PostException;
import com.gotcha.domain.post.repository.PostCommentLikeRepository;
import com.gotcha.domain.post.repository.PostCommentRepository;
import com.gotcha.domain.post.repository.PostRepository;
import com.gotcha.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostCommentLikeService {

    private final PostCommentLikeRepository postCommentLikeRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostRepository postRepository;
    private final SecurityUtil securityUtil;

    @Transactional
    public PostCommentLikeResponse addLike(Long postId, Long commentId) {
        User currentUser = securityUtil.getCurrentUser();

        if (!postRepository.existsById(postId)) {
            throw PostException.notFound();
        }

        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(PostException::commentNotFound);

        if (!comment.getPost().getId().equals(postId)) {
            throw PostException.commentNotFound();
        }

        if (postCommentLikeRepository.findByUserIdAndPostCommentId(currentUser.getId(), commentId).isPresent()) {
            throw PostException.commentAlreadyLiked();
        }

        postCommentLikeRepository.save(PostCommentLike.builder()
                .user(currentUser)
                .postComment(comment)
                .build());

        log.info("Post comment like added - userId: {}, commentId: {}", currentUser.getId(), commentId);
        return PostCommentLikeResponse.of(commentId, true);
    }

    @Transactional
    public PostCommentLikeResponse removeLike(Long postId, Long commentId) {
        User currentUser = securityUtil.getCurrentUser();

        if (!postRepository.existsById(postId)) {
            throw PostException.notFound();
        }

        if (!postCommentRepository.existsById(commentId)) {
            throw PostException.commentNotFound();
        }

        PostCommentLike like = postCommentLikeRepository
                .findByUserIdAndPostCommentId(currentUser.getId(), commentId)
                .orElseThrow(PostException::commentLikeNotFound);

        postCommentLikeRepository.delete(like);

        log.info("Post comment like removed - userId: {}, commentId: {}", currentUser.getId(), commentId);
        return PostCommentLikeResponse.of(commentId, false);
    }
}
