package com.startup.domain.auth.entity;

import com.startup.common.entity.BaseEntity;
import com.startup.domain.auth.enums.AuthProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "user_oauth_accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_oauth_provider_user", columnNames = {"provider", "provider_user_id"}),
                @UniqueConstraint(name = "uk_user_oauth_user_provider", columnNames = {"user_id", "provider"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserOAuthAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuthProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 191)
    private String providerUserId;

    @Column(length = 255)
    private String email;

    @Column(length = 100)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Builder
    private UserOAuthAccount(Long userId, AuthProvider provider, String providerUserId,
                             String email, String nickname, String profileImageUrl) {
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        updateProfile(email, nickname, profileImageUrl);
    }

    public void updateProfile(String email, String nickname, String profileImageUrl) {
        this.email = normalizeBlank(email);
        this.nickname = normalizeBlank(nickname);
        this.profileImageUrl = normalizeBlank(profileImageUrl);
    }

    private static String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
