package com.startup.domain.auth.dto;

import com.startup.domain.auth.enums.AuthProvider;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OAuthLoginRequest(
        @NotNull(message = "provider is required.")
        AuthProvider provider,

        String idToken,

        String accessToken,

        @Size(max = 100, message = "deviceId must be 100 characters or less.")
        String deviceId
) {
}
