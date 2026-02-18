package com.gotcha.domain.block.repository;

import com.gotcha.domain.block.entity.UserBlock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    Optional<UserBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("SELECT ub.blocked.id FROM UserBlock ub WHERE ub.blocker.id = :blockerId")
    List<Long> findBlockedUserIdsByBlockerId(@Param("blockerId") Long blockerId);

    @Query("SELECT ub FROM UserBlock ub JOIN FETCH ub.blocked " +
           "WHERE ub.blocker.id = :blockerId ORDER BY ub.createdAt DESC")
    Page<UserBlock> findAllByBlockerIdWithBlocked(@Param("blockerId") Long blockerId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserBlock ub WHERE ub.blocker.id = :blockerId AND ub.blocked.id = :blockedId")
    void deleteByBlockerIdAndBlockedId(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserBlock ub WHERE ub.blocker.id = :userId OR ub.blocked.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
