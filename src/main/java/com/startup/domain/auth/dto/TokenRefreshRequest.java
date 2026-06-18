package com.startup.domain.auth.dto;

import jakarta.validation.constraints.Size;

public record TokenRefreshRequest(
        @Size(max = 4096, message = "refreshToken is too long.")
        String refreshToken,

        @Size(max = 100, message = "deviceId must be 100 characters or less.")
        String deviceId
) {
}
