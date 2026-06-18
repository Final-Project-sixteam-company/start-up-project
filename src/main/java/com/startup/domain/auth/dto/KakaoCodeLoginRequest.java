package com.startup.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KakaoCodeLoginRequest(
        @NotBlank(message = "authorizationCode is required.")
        String authorizationCode,

        @NotBlank(message = "redirectUri is required.")
        String redirectUri,

        @Size(max = 100, message = "deviceId must be 100 characters or less.")
        String deviceId
) {
}
