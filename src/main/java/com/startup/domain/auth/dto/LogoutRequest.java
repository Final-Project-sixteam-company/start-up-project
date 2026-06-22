package com.startup.domain.auth.dto;

import jakarta.validation.constraints.Size;

public record LogoutRequest(
        @Size(max = 4096, message = "refreshToken is too long.")
        String refreshToken
) {
}
