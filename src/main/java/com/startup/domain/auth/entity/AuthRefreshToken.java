package com.startup.domain.auth.entity;

import com.startup.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "auth_refresh_tokens",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_auth_refresh_tokens_hash", columnNames = {"token_hash"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthRefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "rotated_from_id")
    private Long rotatedFromId;

    @Builder
    private AuthRefreshToken(Long userId, String tokenHash, String deviceId,
                             LocalDateTime expiresAt, Long rotatedFromId) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.deviceId = normalizeBlank(deviceId);
        this.expiresAt = expiresAt;
        this.rotatedFromId = rotatedFromId;
    }

    public boolean isUsable(LocalDateTime now) {
        return this.revokedAt == null && this.expiresAt.isAfter(now);
    }

    public boolean isExpired(LocalDateTime now) {
        return !this.expiresAt.isAfter(now);
    }

    public void revoke() {
        if (this.revokedAt == null) {
            this.revokedAt = LocalDateTime.now();
        }
    }

    private static String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
