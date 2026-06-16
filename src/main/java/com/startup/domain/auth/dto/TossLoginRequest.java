package com.startup.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TossLoginRequest(
        @NotBlank(message = "authorizationCode is required.")
        String authorizationCode,

        @NotBlank(message = "referrer is required.")
        String referrer,

        @NotBlank(message = "deviceId is required.")
        @Size(max = 100, message = "deviceId must be 100 characters or less.")
        String deviceId
) {
}
