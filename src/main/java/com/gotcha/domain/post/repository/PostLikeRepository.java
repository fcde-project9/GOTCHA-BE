package com.gotcha.domain.post.repository;

import com.gotcha.domain.post.entity.PostLike;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByUserIdAndPostId(Long userId, Long postId);

    Long countByPostId(Long postId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PostLike pl WHERE pl.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PostLike pl WHERE pl.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);

    @Query("SELECT pl.post.id AS postId, COUNT(pl) AS likeCount " +
            "FROM PostLike pl WHERE pl.post.id IN :postIds GROUP BY pl.post.id")
    List<PostLikeCount> countByPostIdIn(@Param("postIds") List<Long> postIds);

    interface PostLikeCount {
        Long getPostId();
        Long getLikeCount();
    }
}
