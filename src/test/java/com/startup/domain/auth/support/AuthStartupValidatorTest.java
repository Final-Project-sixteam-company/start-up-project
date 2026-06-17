package com.startup.domain.auth.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthStartupValidatorTest {

    @Test
    void validateAllowsDefaultCompatibilityModeWithoutJwtSecret() {
        AuthProperties authProperties = new AuthProperties();
        AuthStartupValidator validator = new AuthStartupValidator(authProperties);

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void validateRejectsProtectedModeWithoutJwtSecret() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.setRequireAuthentication(true);
        AuthStartupValidator validator = new AuthStartupValidator(authProperties);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void validateRejectsOauthProviderConfigWithoutJwtSecret() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.getOauth().getGoogle().setClientIds("google-client-id");
        AuthStartupValidator validator = new AuthStartupValidator(authProperties);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void validateRejectsTossMtlsConfigWithoutJwtSecret() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.getOauth().getToss().setMtlsCertPath("/opt/clueroom/secrets/toss/client.crt");
        authProperties.getOauth().getToss().setMtlsKeyPath("/opt/clueroom/secrets/toss/client.key");
        AuthStartupValidator validator = new AuthStartupValidator(authProperties);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void validateAllowsAuthFeaturesWhenJwtSecretIsConfigured() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.setRequireAuthentication(true);
        authProperties.getJwt().setSecret("12345678901234567890123456789012");
        AuthStartupValidator validator = new AuthStartupValidator(authProperties);

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }
}
