package com.startup.domain.auth.entity;

import com.startup.common.entity.BaseEntity;
import com.startup.domain.auth.enums.UserRole;
import com.startup.domain.auth.enums.UserStatus;
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
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = {"email"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status;

    @Builder
    private User(String email, String nickname, String profileImageUrl, UserRole role, UserStatus status) {
        this.email = normalizeBlank(email);
        this.nickname = normalizeNickname(nickname);
        this.profileImageUrl = normalizeBlank(profileImageUrl);
        this.role = role == null ? UserRole.USER : role;
        this.status = status == null ? UserStatus.ACTIVE : status;
    }

    public void updateProfile(String email, String nickname, String profileImageUrl) {
        this.email = normalizeBlank(email);
        this.nickname = normalizeNickname(nickname);
        this.profileImageUrl = normalizeBlank(profileImageUrl);
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    private static String normalizeNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return "ClueRoom User";
        }
        return nickname.trim();
    }

    private static String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
