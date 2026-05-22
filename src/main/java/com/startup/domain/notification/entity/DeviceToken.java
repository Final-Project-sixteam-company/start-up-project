package com.startup.domain.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "device_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_device_tokens_token", columnNames = "token")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class DeviceToken {

    private static final String DEFAULT_DEVICE_TYPE = "ANDROID";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 512)
    private String token;

    @Column(name = "device_type", nullable = false, length = 30)
    private String deviceType = DEFAULT_DEVICE_TYPE;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private DeviceToken(Long userId, String token, String deviceType) {
        this.userId = userId;
        this.token = token;
        this.deviceType = normalizeDeviceType(deviceType);
        this.active = true;
        this.lastUsedAt = LocalDateTime.now();
    }

    public void register(Long userId, String deviceType) {
        this.userId = userId;
        this.deviceType = normalizeDeviceType(deviceType);
        this.active = true;
        this.lastUsedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
    }

    private static String normalizeDeviceType(String deviceType) {
        if (deviceType == null || deviceType.isBlank()) {
            return DEFAULT_DEVICE_TYPE;
        }
        return deviceType;
    }
}
