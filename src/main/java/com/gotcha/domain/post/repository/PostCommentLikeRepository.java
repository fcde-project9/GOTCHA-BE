package com.gotcha.domain.post.repository;

import com.gotcha.domain.post.entity.PostCommentLike;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostCommentLikeRepository extends JpaRepository<PostCommentLike, Long> {

    Optional<PostCommentLike> findByUserIdAndPostCommentId(Long userId, Long postCommentId);

    Long countByPostCommentId(Long postCommentId);

    @Query("SELECT pcl.postComment.id AS commentId, COUNT(pcl) AS likeCount " +
            "FROM PostCommentLike pcl WHERE pcl.postComment.id IN :commentIds GROUP BY pcl.postComment.id")
    List<PostCommentLikeCount> countByPostCommentIdIn(@Param("commentIds") List<Long> commentIds);

    interface PostCommentLikeCount {
        Long getCommentId();
        Long getLikeCount();
    }

    @Query("SELECT pcl.postComment.id FROM PostCommentLike pcl " +
            "WHERE pcl.user.id = :userId AND pcl.postComment.id IN :commentIds")
    java.util.Set<Long> findLikedCommentIdsByUserIdAndCommentIdIn(
            @Param("userId") Long userId,
            @Param("commentIds") java.util.List<Long> commentIds
    );

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PostCommentLike pcl WHERE pcl.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PostCommentLike pcl WHERE pcl.postComment.id = :postCommentId")
    void deleteAllByPostCommentId(@Param("postCommentId") Long postCommentId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PostCommentLike pcl WHERE pcl.postComment.id IN :commentIds")
    void deleteAllByPostCommentIdIn(@Param("commentIds") java.util.List<Long> commentIds);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PostCommentLike pcl WHERE pcl.postComment.post.id IN :postIds")
    void deleteAllByPostIdIn(@Param("postIds") java.util.List<Long> postIds);
}
