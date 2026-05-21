package com.gotcha.domain.post.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.post.dto.CreatePostCommentRequest;
import com.gotcha.domain.post.dto.CreatePostRequest;
import com.gotcha.domain.post.dto.PostCommentResponse;
import com.gotcha.domain.post.dto.PostCommentLikeResponse;
import com.gotcha.domain.post.dto.PostCursorResponse;
import com.gotcha.domain.post.dto.PostDetailResponse;
import com.gotcha.domain.post.dto.PostLikeResponse;
import com.gotcha.domain.post.dto.PostResponse;
import com.gotcha.domain.post.dto.PostShopInfo;
import com.gotcha.domain.post.dto.PostSortType;
import com.gotcha.domain.post.dto.UpdatePostRequest;
import java.util.List;
import com.gotcha.domain.post.service.PostCommentLikeService;
import com.gotcha.domain.post.service.PostCommentService;
import com.gotcha.domain.post.service.PostLikeService;
import com.gotcha.domain.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController implements PostControllerApi {

    private final PostService postService;
    private final PostLikeService postLikeService;
    private final PostCommentService postCommentService;
    private final PostCommentLikeService postCommentLikeService;
    private final SecurityUtil securityUtil;

    @Override
    @GetMapping("/shops/search")
    public ApiResponse<List<PostShopInfo>> searchShops(@RequestParam String keyword) {
        return ApiResponse.success(postService.searchShopsForPost(keyword));
    }

    @Override
    @GetMapping
    public ApiResponse<PostCursorResponse> getPosts(
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "LATEST") PostSortType sort
    ) {
        if (sort == PostSortType.POPULAR) {
            return ApiResponse.success(postService.getPopularPosts(typeId, page, size));
        }
        return ApiResponse.success(postService.getPostsByCursor(typeId, cursor, size));
    }

    @Override
    @GetMapping("/{postId}")
    public ApiResponse<PostDetailResponse> getPostDetail(@PathVariable Long postId) {
        return ApiResponse.success(postService.getPostDetail(postId));
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostResponse> createPost(@Valid @RequestBody CreatePostRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        return ApiResponse.success(postService.createPost(userId, request));
    }

    @Override
    @PostMapping("/{postId}/like")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostLikeResponse> addLike(@PathVariable Long postId) {
        return ApiResponse.success(postLikeService.addLike(postId));
    }

    @Override
    @DeleteMapping("/{postId}/like")
    public ApiResponse<PostLikeResponse> removeLike(@PathVariable Long postId) {
        return ApiResponse.success(postLikeService.removeLike(postId));
    }

    @Override
    @PutMapping("/{postId}")
    public ApiResponse<PostResponse> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody UpdatePostRequest request
    ) {
        return ApiResponse.success(postService.updatePost(postId, request));
    }

    @Override
    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return ApiResponse.success(null);
    }

    @Override
    @PostMapping("/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostCommentResponse> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CreatePostCommentRequest request
    ) {
        return ApiResponse.success(postCommentService.createComment(postId, request));
    }

    @Override
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        postCommentService.deleteComment(postId, commentId);
        return ApiResponse.success(null);
    }

    @Override
    @PostMapping("/{postId}/comments/{commentId}/like")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostCommentLikeResponse> addCommentLike(
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        return ApiResponse.success(postCommentLikeService.addLike(postId, commentId));
    }

    @Override
    @DeleteMapping("/{postId}/comments/{commentId}/like")
    public ApiResponse<PostCommentLikeResponse> removeCommentLike(
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        return ApiResponse.success(postCommentLikeService.removeLike(postId, commentId));
    }

}
