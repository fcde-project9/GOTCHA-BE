package com.gotcha.domain.user.repository;

import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.entity.UserStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findBySocialTypeAndSocialId(SocialType socialType, String socialId);

    boolean existsBySocialTypeAndSocialId(SocialType socialType, String socialId);

    Optional<User> findByNickname(String nickname);

    boolean existsByNickname(String nickname);

    @Query("SELECT u FROM User u WHERE (:status IS NULL OR u.status = :status) AND u.isDeleted = false")
    Page<User> findAllWithStatusFilter(@Param("status") UserStatus status, Pageable pageable);
}
