package com.startup.domain.auth.support;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AuthCookieSupport {

    private final AuthProperties authProperties;

    public String refreshCookieName() {
        return authProperties.getRefreshCookie().getName();
    }

    public void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        if (!authProperties.getRefreshCookie().isEnabled() || !StringUtils.hasText(refreshToken)) {
            return;
        }
        response.addHeader(HttpHeaders.SET_COOKIE, baseCookie(refreshToken)
                .maxAge(Duration.ofDays(authProperties.getJwt().getRefreshTokenTtlDays()))
                .build()
                .toString());
    }

    public void clearRefreshCookie(HttpServletResponse response) {
        if (!authProperties.getRefreshCookie().isEnabled()) {
            return;
        }
        response.addHeader(HttpHeaders.SET_COOKIE, baseCookie("")
                .maxAge(Duration.ZERO)
                .build()
                .toString());
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        AuthProperties.RefreshCookie cookie = authProperties.getRefreshCookie();
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookie.getName(), value)
                .httpOnly(true)
                .secure(cookie.isSecure())
                .path(cookie.getPath())
                .sameSite(cookie.getSameSite());
        if (StringUtils.hasText(cookie.getDomain())) {
            builder.domain(cookie.getDomain());
        }
        return builder;
    }
}
