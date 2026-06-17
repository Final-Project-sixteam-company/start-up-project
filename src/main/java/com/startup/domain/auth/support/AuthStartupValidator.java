package com.startup.domain.auth.support;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AuthStartupValidator implements ApplicationRunner {

    private final AuthProperties authProperties;

    @Override
    public void run(ApplicationArguments args) {
        validate();
    }

    public void validate() {
        if (requiresJwtSecret() && !authProperties.isJwtSecretConfigured()) {
            throw new IllegalStateException(
                    "JWT_SECRET must be configured with at least 32 characters before enabling auth features."
            );
        }
    }

    private boolean requiresJwtSecret() {
        return authProperties.isRequireAuthentication()
                || authProperties.isDevLoginEnabled()
                || !authProperties.getOauth().getGoogle().clientIdList().isEmpty()
                || StringUtils.hasText(authProperties.getOauth().getKakao().getAppId())
                || authProperties.getOauth().getToss().isMtlsConfigured();
    }
}
