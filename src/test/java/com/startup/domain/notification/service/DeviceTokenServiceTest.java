package com.startup.domain.notification.service;

import com.startup.domain.notification.dto.DeviceTokenRegisterRequest;
import com.startup.domain.notification.dto.DeviceTokenResponse;
import com.startup.domain.notification.entity.DeviceToken;
import com.startup.domain.notification.repository.DeviceTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DeviceTokenServiceTest {

    @Autowired
    private DeviceTokenService deviceTokenService;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Test
    void registerCreatesDeviceToken() {
        DeviceTokenResponse response = deviceTokenService.register(
                1L,
                new DeviceTokenRegisterRequest("new-token", "ANDROID")
        );

        DeviceToken saved = deviceTokenRepository.findByToken("new-token").orElseThrow();

        assertThat(response.deviceTokenId()).isEqualTo(saved.getId());
        assertThat(response.active()).isTrue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getDeviceType()).isEqualTo("ANDROID");
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getLastUsedAt()).isNotNull();
    }

    @Test
    void registerExistingTokenReusesRowAndActivatesIt() {
        DeviceToken existing = deviceTokenRepository.save(DeviceToken.builder()
                .userId(1L)
                .token("existing-token")
                .deviceType("ANDROID")
                .build());
        existing.deactivate();
        LocalDateTime previousLastUsedAt = existing.getLastUsedAt();

        DeviceTokenResponse response = deviceTokenService.register(
                2L,
                new DeviceTokenRegisterRequest("existing-token", "android")
        );
        DeviceToken updated = deviceTokenRepository.findByToken("existing-token").orElseThrow();

        assertThat(response.deviceTokenId()).isEqualTo(existing.getId());
        assertThat(response.active()).isTrue();
        assertThat(updated.getUserId()).isEqualTo(2L);
        assertThat(updated.getDeviceType()).isEqualTo("ANDROID");
        assertThat(updated.isActive()).isTrue();
        assertThat(updated.getLastUsedAt()).isAfterOrEqualTo(previousLastUsedAt);
        assertThat(deviceTokenRepository.findAll()).hasSize(1);
    }

    @Test
    void getActiveTokensReturnsOnlyActiveTokensForUser() {
        deviceTokenRepository.save(DeviceToken.builder()
                .userId(1L)
                .token("active-token")
                .deviceType("ANDROID")
                .build());
        DeviceToken inactive = deviceTokenRepository.save(DeviceToken.builder()
                .userId(1L)
                .token("inactive-token")
                .deviceType("ANDROID")
                .build());
        inactive.deactivate();
        deviceTokenRepository.save(DeviceToken.builder()
                .userId(2L)
                .token("other-user-token")
                .deviceType("ANDROID")
                .build());

        List<String> tokens = deviceTokenService.getActiveTokens(1L);

        assertThat(tokens).containsExactly("active-token");
    }
}
