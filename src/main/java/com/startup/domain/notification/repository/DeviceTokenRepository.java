package com.startup.domain.notification.repository;

import com.startup.domain.notification.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// device_tokens 테이블 접근을 담당하며, 등록 경합 처리는 DB unique key를 기준으로 해결한다.
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    // find-then-save는 동시 최초 등록에서 unique key race가 나므로 DB upsert로 원자 처리한다.
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

    // 테스트 푸시와 실제 발송 모두 비활성 토큰을 제외한 현재 사용자 token만 대상으로 삼는다.
    List<DeviceToken> findAllByUserIdAndActiveTrue(Long userId);
}
