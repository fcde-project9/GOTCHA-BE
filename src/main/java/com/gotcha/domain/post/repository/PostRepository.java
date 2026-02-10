package com.gotcha.domain.post.repository;

import com.gotcha.domain.post.entity.Post;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findAllByUserId(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Post p WHERE p.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
