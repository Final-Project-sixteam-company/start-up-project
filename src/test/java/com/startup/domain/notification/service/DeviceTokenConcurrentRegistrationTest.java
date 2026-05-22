package com.startup.domain.notification.service;

import com.startup.domain.notification.dto.DeviceTokenRegisterRequest;
import com.startup.domain.notification.dto.DeviceTokenResponse;
import com.startup.domain.notification.entity.DeviceToken;
import com.startup.domain.notification.repository.DeviceTokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
// 같은 token 최초 등록이 동시에 들어와도 unique key 예외 대신 하나의 row로 수렴하는지 검증한다.
class DeviceTokenConcurrentRegistrationTest {

    @Autowired
    private DeviceTokenService deviceTokenService;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        // 동시성 테스트는 커밋된 데이터가 남으므로 테스트 전후로 직접 정리한다.
        deviceTokenRepository.deleteAll();
    }

    @Test
    void concurrentFirstRegistrationsOfSameTokenReuseSingleRow() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        // 두 요청이 최대한 같은 시점에 register()로 진입하도록 latch로 시작 시점을 맞춘다.
        Future<DeviceTokenResponse> first = executorService.submit(() -> registerAfterStart(1L, ready, start));
        Future<DeviceTokenResponse> second = executorService.submit(() -> registerAfterStart(2L, ready, start));

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        DeviceTokenResponse firstResponse = first.get(5, TimeUnit.SECONDS);
        DeviceTokenResponse secondResponse = second.get(5, TimeUnit.SECONDS);
        executorService.shutdownNow();

        List<DeviceToken> savedTokens = deviceTokenRepository.findAll();

        assertThat(savedTokens).hasSize(1);
        assertThat(firstResponse.deviceTokenId()).isEqualTo(savedTokens.getFirst().getId());
        assertThat(secondResponse.deviceTokenId()).isEqualTo(savedTokens.getFirst().getId());
        assertThat(savedTokens.getFirst().isActive()).isTrue();
    }

    private DeviceTokenResponse registerAfterStart(Long userId, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        // ready를 먼저 내린 뒤 start 신호까지 대기해 두 스레드의 경쟁 상황을 만든다.
        ready.countDown();
        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
        return deviceTokenService.register(
                userId,
                new DeviceTokenRegisterRequest("concurrent-token", "ANDROID")
        );
    }
}
