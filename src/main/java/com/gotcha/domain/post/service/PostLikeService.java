package com.gotcha.domain.post.service;

import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.post.dto.PostLikeResponse;
import com.gotcha.domain.post.entity.Post;
import com.gotcha.domain.post.entity.PostLike;
import com.gotcha.domain.post.exception.PostException;
import com.gotcha.domain.post.repository.PostLikeRepository;
import com.gotcha.domain.post.repository.PostRepository;
import com.gotcha.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final SecurityUtil securityUtil;

    @Transactional
    public PostLikeResponse addLike(Long postId) {
        User currentUser = securityUtil.getCurrentUser();

        Post post = postRepository.findById(postId)
                .orElseThrow(PostException::notFound);

        if (postLikeRepository.findByUserIdAndPostId(currentUser.getId(), postId).isPresent()) {
            throw PostException.alreadyLiked();
        }

        try {
            postLikeRepository.save(PostLike.builder()
                    .user(currentUser)
                    .post(post)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw PostException.alreadyLiked();
        }

        log.info("Post like added - userId: {}, postId: {}", currentUser.getId(), postId);
        return PostLikeResponse.of(postId, true);
    }

    @Transactional
    public PostLikeResponse removeLike(Long postId) {
        User currentUser = securityUtil.getCurrentUser();

        if (!postRepository.existsById(postId)) {
            throw PostException.notFound();
        }

        PostLike postLike = postLikeRepository.findByUserIdAndPostId(currentUser.getId(), postId)
                .orElseThrow(PostException::likeNotFound);

        postLikeRepository.delete(postLike);

        log.info("Post like removed - userId: {}, postId: {}", currentUser.getId(), postId);
        return PostLikeResponse.of(postId, false);
    }
}
