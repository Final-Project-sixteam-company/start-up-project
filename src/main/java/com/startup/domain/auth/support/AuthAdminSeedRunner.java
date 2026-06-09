package com.startup.domain.auth.support;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthAdminSeedRunner implements ApplicationRunner {

    private final AuthAdminSeedService authAdminSeedService;

    @Override
    public void run(ApplicationArguments args) {
        authAdminSeedService.seedIfEnabled();
    }
}
