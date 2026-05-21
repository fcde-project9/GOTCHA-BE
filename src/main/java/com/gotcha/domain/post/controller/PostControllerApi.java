package com.gotcha.domain.post.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha._global.common.PageResponse;
import com.gotcha.domain.post.dto.CreatePostCommentRequest;
import com.gotcha.domain.post.dto.CreatePostRequest;
import com.gotcha.domain.post.dto.PostCommentDetailResponse;
import com.gotcha.domain.post.dto.PostCommentLikeResponse;
import com.gotcha.domain.post.dto.PostCommentResponse;
import com.gotcha.domain.post.dto.PostCursorResponse;
import com.gotcha.domain.post.dto.PostDetailResponse;
import com.gotcha.domain.post.dto.PostLikeResponse;
import com.gotcha.domain.post.dto.PostResponse;
import com.gotcha.domain.post.dto.PostShopInfo;
import com.gotcha.domain.post.dto.PostSortType;
import com.gotcha.domain.post.dto.UpdatePostRequest;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Post", description = "커뮤니티 게시글 API")
public interface PostControllerApi {

    @Operation(summary = "게시글 작성용 매장 검색",
            description = "게시글 작성 시 연결할 매장을 검색합니다. 매장 이름으로 부분 검색하며 최대 50개를 반환합니다. " +
                    "검색어가 2자 미만이면 빈 배열을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공")
    })
    ApiResponse<List<PostShopInfo>> searchShops(
            @Parameter(description = "검색어 (매장 이름, 2자 이상)") @RequestParam String keyword
    );

    @Operation(summary = "게시글 목록 조회",
            description = "커뮤니티 게시글 목록을 조회합니다. sort 값에 따라 동작이 달라집니다. " +
                    "LATEST(기본): 최신순 커서 기반 무한 스크롤 — `cursor`와 `size` 사용. " +
                    "POPULAR: 최근 7일간 좋아요 많은 순 페이지 기반 — `page`와 `size` 사용. " +
                    "typeId 지정 시 해당 카테고리만 조회됩니다. " +
                    "응답의 hasNext로 다음 데이터 존재 여부를 판단합니다 (LATEST는 nextCursor도 함께 사용).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ApiResponse<PostCursorResponse> getPosts(
            @Parameter(description = "카테고리 ID (없으면 전체 조회)") @RequestParam(required = false) Long typeId,
            @Parameter(description = "마지막으로 받은 게시글 ID (LATEST 정렬에서만 사용)") @RequestParam(required = false) Long cursor,
            @Parameter(description = "페이지 번호 (POPULAR 정렬에서만 사용, 기본 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "조회 개수 (기본 20)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 방식: LATEST(기본, 최신순) 또는 POPULAR(인기순, 최근 7일)") @RequestParam(defaultValue = "LATEST") PostSortType sort
    );

    @Operation(summary = "게시글 상세 조회",
            description = "게시글 상세 정보를 조회합니다. 댓글 및 대댓글을 포함합니다. 로그인 시 좋아요 여부와 본인 작성 여부를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "게시글을 찾을 수 없음 (PT001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<PostDetailResponse> getPostDetail(@PathVariable Long postId);

    @Operation(summary = "게시글 작성",
            description = "커뮤니티 게시글을 작성합니다. 카테고리 선택 필수, 이미지 최대 5개",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "게시글 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "잘못된 요청 (PT002: 카테고리 없음, PT003: 이미지 개수 초과)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "로그인 필요 (A001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<PostResponse> createPost(@Valid @RequestBody CreatePostRequest request);

    @Operation(summary = "게시글 수정",
            description = "게시글을 수정합니다. 작성자 본인만 가능합니다. 이미지는 전체 교체됩니다 (기존 이미지는 S3에서 삭제). " +
                    "공개 여부(isPublic)는 작성 시점에만 설정 가능하며 수정으로는 변경할 수 없습니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "잘못된 요청 (PT002: 카테고리 없음, PT003: 이미지 개수 초과)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "로그인 필요 (A001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "본인의 게시글만 수정 가능 (PT004)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "게시글을 찾을 수 없음 (PT001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<PostResponse> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody UpdatePostRequest request
    );

    @Operation(summary = "게시글 삭제",
            description = "게시글을 삭제합니다. 작성자 본인 또는 ADMIN만 가능합니다. 이미지(S3), 댓글, 좋아요가 함께 삭제됩니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "로그인 필요 (A001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "본인의 게시글만 삭제 가능 (PT004)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "게시글을 찾을 수 없음 (PT001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<Void> deletePost(@PathVariable Long postId);

    @Operation(summary = "게시글 좋아요",
            description = "게시글에 좋아요를 추가합니다",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "좋아요 추가 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "로그인 필요 (A001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "게시글을 찾을 수 없음 (PT001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "이미 좋아요한 게시글 (PT005)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<PostLikeResponse> addLike(@PathVariable Long postId);

    @Operation(summary = "게시글 좋아요 취소",
            description = "게시글 좋아요를 취소합니다",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "로그인 필요 (A001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "좋아요 정보를 찾을 수 없음 (PT006)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<PostLikeResponse> removeLike(@PathVariable Long postId);

    @Operation(summary = "게시글 댓글/대댓글 작성",
            description = "게시글에 댓글 또는 대댓글을 작성합니다. parentId가 없으면 댓글, 있으면 대댓글입니다. 대댓글의 대댓글은 불가합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "댓글 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "잘못된 요청 (PT009: 대댓글에 대댓글 불가)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "로그인 필요 (A001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "게시글 또는 부모 댓글을 찾을 수 없음 (PT001, PT007)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<PostCommentResponse> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CreatePostCommentRequest request
    );

    @Operation(summary = "게시글 댓글/대댓글 삭제",
            description = "게시글 댓글 또는 대댓글을 삭제합니다. 작성자 본인만 가능합니다. 댓글 삭제 시 하위 대댓글도 함께 삭제됩니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "로그인 필요 (A001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "본인의 댓글만 삭제 가능 (PT008)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "게시글 또는 댓글을 찾을 수 없음 (PT001, PT007)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId
    );

    @Operation(summary = "게시글 댓글 좋아요",
            description = "게시글 댓글에 좋아요를 추가합니다",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "좋아요 추가 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "로그인 필요 (A001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "게시글 또는 댓글을 찾을 수 없음 (PT001, PT007)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "이미 좋아요한 댓글 (PT010)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<PostCommentLikeResponse> addCommentLike(
            @PathVariable Long postId,
            @PathVariable Long commentId
    );

    @Operation(summary = "게시글 댓글 좋아요 취소",
            description = "게시글 댓글 좋아요를 취소합니다",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "로그인 필요 (A001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "좋아요 정보를 찾을 수 없음 (PT011)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    ApiResponse<PostCommentLikeResponse> removeCommentLike(
            @PathVariable Long postId,
            @PathVariable Long commentId
    );
}
