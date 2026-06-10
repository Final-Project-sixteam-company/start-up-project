package com.startup.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TokenRefreshRequest(
        @NotBlank(message = "refreshToken is required.")
        String refreshToken,

        @Size(max = 100, message = "deviceId must be 100 characters or less.")
        String deviceId
) {
}
