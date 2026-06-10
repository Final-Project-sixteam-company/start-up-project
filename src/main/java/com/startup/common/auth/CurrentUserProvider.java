package com.startup.common.auth;

import com.startup.common.error.BusinessException;
import com.startup.common.error.CommonErrorCode;
import com.startup.domain.auth.support.AuthProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CurrentUserProvider {

    private final Long mockUserId;
    private final AuthProperties authProperties;

    public CurrentUserProvider(
            @Value("${app.mock-user-id:1}") Long mockUserId,
            AuthProperties authProperties
    ) {
        this.mockUserId = mockUserId;
        this.authProperties = authProperties;
    }

    public Long currentUserId() {
        return authenticatedPrincipal()
                .map(AuthenticatedUserPrincipal::userId)
                .orElseGet(this::fallbackUserId);
    }

    public Long currentUserIdOrNull() {
        return authenticatedPrincipal()
                .map(AuthenticatedUserPrincipal::userId)
                .orElseGet(() -> isLegacyCompatibilityMode() ? mockUserId : null);
    }

    public AuthenticatedUserPrincipal requireAuthenticatedPrincipal() {
        return authenticatedPrincipal()
                .orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED));
    }

    public Optional<AuthenticatedUserPrincipal> authenticatedPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUserPrincipal authenticatedUserPrincipal) {
            return Optional.of(authenticatedUserPrincipal);
        }
        return Optional.empty();
    }

    private Long fallbackUserId() {
        if (!isLegacyCompatibilityMode()) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return mockUserId;
    }

    private boolean isLegacyCompatibilityMode() {
        return !authProperties.isRequireAuthentication() && authProperties.isMockFallbackEnabled();
    }
}
