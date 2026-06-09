package com.startup.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DevLoginRequest(
        @NotBlank(message = "email is required.")
        @Email(message = "email format is invalid.")
        @Size(max = 255, message = "email must be 255 characters or less.")
        String email,

        @Size(max = 100, message = "nickname must be 100 characters or less.")
        String nickname,

        @Size(max = 100, message = "deviceId must be 100 characters or less.")
        String deviceId
) {
}
