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
// 디바이스 토큰 등록/upsert와 활성 token 조회 정책을 실제 Repository로 검증한다.
class DeviceTokenServiceTest {

    @Autowired
    private DeviceTokenService deviceTokenService;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Test
    void registerCreatesDeviceToken() {
        // 처음 보는 token은 active=true 상태로 새 row를 만든다.
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
        // 이미 저장된 token은 새 row를 만들지 않고 기존 row를 최신 사용자/활성 상태로 갱신한다.
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
        // 발송 대상 조회에서는 다른 사용자 token과 비활성 token을 제외한다.
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
