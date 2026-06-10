package com.startup.domain.auth.repository;

import com.startup.domain.auth.entity.AuthRefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, Long> {

    Optional<AuthRefreshToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from AuthRefreshToken token where token.tokenHash = :tokenHash")
    Optional<AuthRefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AuthRefreshToken token
            set token.revokedAt = :revokedAt
            where token.userId = :userId
              and token.deviceId = :deviceId
              and token.revokedAt is null
              and token.expiresAt > :now
            """)
    int revokeActiveByUserIdAndDeviceId(
            @Param("userId") Long userId,
            @Param("deviceId") String deviceId,
            @Param("now") LocalDateTime now,
            @Param("revokedAt") LocalDateTime revokedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AuthRefreshToken token
            set token.revokedAt = :revokedAt
            where token.userId = :userId
              and token.deviceId is null
              and token.revokedAt is null
              and token.expiresAt > :now
            """)
    int revokeActiveByUserIdAndNullDeviceId(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now,
            @Param("revokedAt") LocalDateTime revokedAt
    );
}
