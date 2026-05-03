package com.gotcha.domain.post.repository;

import com.gotcha.domain.post.entity.Post;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 페이지 기반 (정렬: createdAt DESC) — 비공개 글은 작성자 본인 또는 ADMIN만 노출
    @EntityGraph(attributePaths = {"user", "type", "shop"})
    @Query("SELECT p FROM Post p "
            + "WHERE (p.isPublic = true "
            + "       OR :isAdmin = true "
            + "       OR (:currentUserId IS NOT NULL AND p.user.id = :currentUserId)) "
            + "ORDER BY p.createdAt DESC")
    Page<Post> findVisibleAll(@Param("currentUserId") Long currentUserId,
                              @Param("isAdmin") boolean isAdmin,
                              Pageable pageable);

    @EntityGraph(attributePaths = {"user", "type", "shop"})
    @Query("SELECT p FROM Post p "
            + "WHERE p.type.id = :typeId "
            + "AND (p.isPublic = true "
            + "     OR :isAdmin = true "
            + "     OR (:currentUserId IS NOT NULL AND p.user.id = :currentUserId)) "
            + "ORDER BY p.createdAt DESC")
    Page<Post> findVisibleByTypeId(@Param("typeId") Long typeId,
                                   @Param("currentUserId") Long currentUserId,
                                   @Param("isAdmin") boolean isAdmin,
                                   Pageable pageable);

    // Cursor 기반 (id DESC = 최신순) — 비공개 글은 작성자 본인 또는 ADMIN만 노출
    @EntityGraph(attributePaths = {"user", "type", "shop"})
    @Query("SELECT p FROM Post p "
            + "WHERE (:cursorId IS NULL OR p.id < :cursorId) "
            + "AND (:typeId IS NULL OR p.type.id = :typeId) "
            + "AND (p.isPublic = true "
            + "     OR :isAdmin = true "
            + "     OR (:currentUserId IS NOT NULL AND p.user.id = :currentUserId)) "
            + "ORDER BY p.id DESC")
    List<Post> findVisibleByCursor(@Param("typeId") Long typeId,
                                   @Param("cursorId") Long cursorId,
                                   @Param("currentUserId") Long currentUserId,
                                   @Param("isAdmin") boolean isAdmin,
                                   Pageable pageable);

    // 인기글 (since 이후 작성된 게시글 중 좋아요 수 많은 순) — 비공개 글은 작성자 본인 또는 ADMIN만 노출
    @EntityGraph(attributePaths = {"user", "type", "shop"})
    @Query(
            value = "SELECT p FROM Post p LEFT JOIN PostLike pl ON pl.post = p "
                    + "WHERE p.createdAt >= :since "
                    + "AND (p.isPublic = true "
                    + "     OR :isAdmin = true "
                    + "     OR (:currentUserId IS NOT NULL AND p.user.id = :currentUserId)) "
                    + "AND (:typeId IS NULL OR p.type.id = :typeId) "
                    + "GROUP BY p "
                    + "ORDER BY COUNT(pl) DESC, p.id DESC",
            countQuery = "SELECT COUNT(p) FROM Post p "
                    + "WHERE p.createdAt >= :since "
                    + "AND (p.isPublic = true "
                    + "     OR :isAdmin = true "
                    + "     OR (:currentUserId IS NOT NULL AND p.user.id = :currentUserId)) "
                    + "AND (:typeId IS NULL OR p.type.id = :typeId)"
    )
    Page<Post> findPopularPosts(@Param("typeId") Long typeId,
                                @Param("currentUserId") Long currentUserId,
                                @Param("isAdmin") boolean isAdmin,
                                @Param("since") LocalDateTime since,
                                Pageable pageable);

    List<Post> findAllByUserId(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Post p WHERE p.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
