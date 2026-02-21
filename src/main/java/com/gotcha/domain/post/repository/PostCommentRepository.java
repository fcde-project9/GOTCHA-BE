package com.gotcha.domain.post.repository;

import com.gotcha.domain.post.entity.PostComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PostComment pc SET pc.parent = null WHERE pc.parent.user.id = :userId")
    void clearParentByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PostComment pc WHERE pc.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PostComment pc WHERE pc.post.id IN :postIds")
    void deleteByPostIdIn(@Param("postIds") List<Long> postIds);
}
