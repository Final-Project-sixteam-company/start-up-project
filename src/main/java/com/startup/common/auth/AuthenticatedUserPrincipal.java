package com.startup.common.auth;

import com.startup.domain.auth.enums.UserRole;

public record AuthenticatedUserPrincipal(
        Long userId,
        String email,
        UserRole role
) {
}
