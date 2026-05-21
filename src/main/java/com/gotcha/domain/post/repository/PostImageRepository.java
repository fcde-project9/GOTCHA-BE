package com.gotcha.domain.post.repository;

import com.gotcha.domain.post.entity.PostImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findAllByPostIdOrderByDisplayOrder(Long postId);

    @Query("SELECT pi FROM PostImage pi WHERE pi.post.id IN :postIds ORDER BY pi.post.id ASC, pi.displayOrder ASC")
    List<PostImage> findAllByPostIdInOrderByDisplayOrder(@Param("postIds") List<Long> postIds);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PostImage pi WHERE pi.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);
}
