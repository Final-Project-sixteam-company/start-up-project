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
// MVP에서는 인증 전환 전까지 userId보다 FCM token을 단일 식별 기준으로 삼아 중복 등록을 막는다.
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
    // 현재는 MockUserProvider 값이 들어가며, JWT 도입 후 실제 사용자 ID로 교체된다.
    private Long userId;

    @Column(nullable = false, length = 512)
    // Firebase registration token은 재발급될 수 있으므로 등록 API를 여러 번 호출해도 upsert된다.
    private String token;

    @Column(name = "device_type", nullable = false, length = 30)
    // MVP 클라이언트는 Android만 대상으로 하지만 추후 iOS 확장을 위해 문자열 컬럼으로 둔다.
    private String deviceType = DEFAULT_DEVICE_TYPE;

    @Column(name = "is_active", nullable = false)
    // 실패 토큰 정리 기능이 붙으면 이 값을 false로 내려 발송 대상에서 제외한다.
    private boolean active = true;

    @Column(name = "last_used_at")
    // 같은 token이 재등록될 때마다 갱신해 최근 사용 여부를 판단할 수 있게 둔다.
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
        // 같은 token이 다시 들어오면 현재 사용자와 디바이스 정보를 최신값으로 덮어쓴다.
        this.userId = userId;
        this.deviceType = normalizeDeviceType(deviceType);
        this.active = true;
        this.lastUsedAt = LocalDateTime.now();
    }

    public void deactivate() {
        // Firebase 발송 실패 토큰 cleanup이 도입되면 이 메서드로 비활성화한다.
        this.active = false;
    }

    private static String normalizeDeviceType(String deviceType) {
        if (deviceType == null || deviceType.isBlank()) {
            return DEFAULT_DEVICE_TYPE;
        }
        return deviceType;
    }
}
