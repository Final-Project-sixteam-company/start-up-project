package com.startup.domain.auth.support;

import com.startup.domain.auth.enums.AuthProvider;

public record OAuthUserProfile(
        AuthProvider provider,
        String providerUserId,
        String email,
        boolean emailVerified,
        String nickname,
        String profileImageUrl
) {
}
