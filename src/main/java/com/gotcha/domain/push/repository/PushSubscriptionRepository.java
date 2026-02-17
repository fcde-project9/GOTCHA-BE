package com.gotcha.domain.push.repository;

import com.gotcha.domain.push.entity.PushSubscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    Optional<PushSubscription> findByEndpoint(String endpoint);

    List<PushSubscription> findAllByUserId(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PushSubscription p WHERE p.endpoint = :endpoint")
    void deleteByEndpoint(@Param("endpoint") String endpoint);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM PushSubscription p WHERE p.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    boolean existsByEndpoint(String endpoint);
}
