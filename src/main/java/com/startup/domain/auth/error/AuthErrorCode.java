package com.startup.domain.auth.error;

import com.startup.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    DEV_LOGIN_DISABLED(HttpStatus.FORBIDDEN, "AUTH_001", "Dev login is disabled."),
    JWT_SECRET_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_002", "JWT secret is not configured."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "Invalid token."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_004", "Refresh token is not valid."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_005", "Refresh token is expired."),
    USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_006", "User not found."),
    USER_NOT_ACTIVE(HttpStatus.FORBIDDEN, "AUTH_007", "User is not active."),
    OAUTH_PROVIDER_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "AUTH_008", "OAuth provider is not supported."),
    OAUTH_TOKEN_REQUIRED(HttpStatus.BAD_REQUEST, "AUTH_009", "OAuth token is required."),
    OAUTH_PROVIDER_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_010", "OAuth provider is not configured."),
    OAUTH_TOKEN_VERIFICATION_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_011", "OAuth token verification failed."),
    OAUTH_ACCOUNT_CONFLICT(HttpStatus.CONFLICT, "AUTH_012", "OAuth account is already linked to another user.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
