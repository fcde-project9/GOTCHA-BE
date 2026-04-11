package com.gotcha.domain.post.repository;

import com.gotcha.domain.post.entity.Post;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = {"user", "type"})
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "type"})
    Page<Post> findAllByTypeIdOrderByCreatedAtDesc(Long typeId, Pageable pageable);

    // Cursor 기반 조회 (id DESC = 최신순)
    @EntityGraph(attributePaths = {"user", "type"})
    List<Post> findAllByOrderByIdDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "type"})
    List<Post> findAllByIdLessThanOrderByIdDesc(Long cursorId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "type"})
    List<Post> findAllByTypeIdOrderByIdDesc(Long typeId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "type"})
    List<Post> findAllByTypeIdAndIdLessThanOrderByIdDesc(Long typeId, Long cursorId, Pageable pageable);

    List<Post> findAllByUserId(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Post p WHERE p.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
