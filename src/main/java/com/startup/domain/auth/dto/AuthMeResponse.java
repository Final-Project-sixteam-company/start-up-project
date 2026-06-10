package com.startup.domain.auth.dto;

import com.startup.domain.auth.enums.UserRole;

public record AuthMeResponse(
        Long userId,
        String email,
        String nickname,
        String profileImageUrl,
        UserRole role
) {
}
