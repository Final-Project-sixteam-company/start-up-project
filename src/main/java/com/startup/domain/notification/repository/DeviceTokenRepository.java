package com.startup.domain.notification.repository;

import com.startup.domain.notification.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO device_tokens (
                user_id, token, device_type, is_active, last_used_at, created_at, updated_at
            )
            VALUES (
                :userId, :token, :deviceType, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON DUPLICATE KEY UPDATE
                user_id = VALUES(user_id),
                device_type = VALUES(device_type),
                is_active = TRUE,
                last_used_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    void upsertActiveToken(
            @Param("userId") Long userId,
            @Param("token") String token,
            @Param("deviceType") String deviceType
    );

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findAllByUserIdAndActiveTrue(Long userId);
}
