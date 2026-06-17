package com.startup.domain.auth.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TossLoginRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void requiresAuthorizationCodeReferrerAndDeviceId() {
        TossLoginRequest request = new TossLoginRequest("", " ", null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("authorizationCode", "referrer", "deviceId");
    }

    @Test
    void rejectsTooLongDeviceId() {
        TossLoginRequest request = new TossLoginRequest("code", "DEFAULT", "a".repeat(101));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("deviceId");
    }
}
