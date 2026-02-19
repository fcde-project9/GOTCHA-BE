package com.gotcha.domain.push.repository;

import com.gotcha.domain.push.entity.DevicePlatform;
import com.gotcha.domain.push.entity.DeviceToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    List<DeviceToken> findAllByUserId(Long userId);

    Optional<DeviceToken> findByDeviceToken(String deviceToken);

    boolean existsByUserIdAndDeviceToken(Long userId, String deviceToken);

    List<DeviceToken> findAllByUserIdAndPlatform(Long userId, DevicePlatform platform);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM DeviceToken d WHERE d.user.id = :userId AND d.deviceToken = :deviceToken")
    void deleteByUserIdAndDeviceToken(@Param("userId") Long userId, @Param("deviceToken") String deviceToken);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM DeviceToken d WHERE d.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
