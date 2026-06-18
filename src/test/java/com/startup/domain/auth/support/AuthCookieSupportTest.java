package com.startup.domain.auth.support;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieSupportTest {

    @Test
    void addRefreshCookieUsesHttpOnlySecureSameSiteDefaults() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.getJwt().setRefreshTokenTtlDays(7);
        AuthCookieSupport support = new AuthCookieSupport(authProperties);
        MockHttpServletResponse response = new MockHttpServletResponse();

        support.addRefreshCookie(response, "refresh-token");

        List<String> cookies = response.getHeaders("Set-Cookie");
        assertThat(cookies).hasSize(1);
        assertThat(cookies.getFirst())
                .contains("clueroom_refresh_token=refresh-token")
                .contains("Path=/api/auth")
                .contains("Max-Age=604800")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=Lax");
    }

    @Test
    void clearRefreshCookieExpiresCookie() {
        AuthCookieSupport support = new AuthCookieSupport(new AuthProperties());
        MockHttpServletResponse response = new MockHttpServletResponse();

        support.clearRefreshCookie(response);

        assertThat(response.getHeaders("Set-Cookie").getFirst())
                .contains("clueroom_refresh_token=")
                .contains("Max-Age=0");
    }
}
