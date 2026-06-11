package com.startup.domain.auth.dto;

import com.startup.domain.auth.enums.UserRole;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserSummary user
) {

    public record UserSummary(
            Long userId,
            String email,
            String nickname,
            String profileImageUrl,
            UserRole role
    ) {
    }
}
