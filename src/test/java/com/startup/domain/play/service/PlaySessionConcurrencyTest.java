package com.startup.domain.play.service;

import com.startup.domain.play.dto.PlaySessionCreateRequest;
import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.error.PlayErrorCode;
import com.startup.domain.play.error.PlayException;
import com.startup.domain.play.repository.PlaySessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PlaySessionConcurrencyTest {

    @Autowired
    private PlaySessionService playSessionService;

    @Autowired
    private PlaySessionRepository playSessionRepository;

    @Autowired
    private com.startup.domain.scenario.repository.ScenarioRepository scenarioRepository;

    @AfterEach
    void tearDown() {
        playSessionRepository.deleteAllInBatch();
        scenarioRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("동시 요청으로 세션 생성을 시도해도 하나만 생성되고 나머지는 예외가 발생한다")
    void createSession_Concurrency() throws InterruptedException {

        // given
        com.startup.domain.scenario.entity.Scenario scenario = scenarioRepository.save(
                com.startup.domain.scenario.entity.Scenario.builder()
                        .title("테스트 시나리오")
                        .description("테스트 시나리오입니다.")
                        .scenarioType(com.startup.domain.scenario.enums.ScenarioType.OFFICIAL)
                        .visibility(com.startup.domain.scenario.enums.ScenarioVisibility.PUBLIC)
                        .difficulty(com.startup.domain.scenario.enums.Difficulty.NORMAL)
                        .status(com.startup.domain.scenario.enums.ScenarioStatus.PUBLISHED)
                        .build()
        );

        Long userId = 999L;
        Long scenarioId = scenario.getId(); // 저장된 시나리오 ID 사용
        PlaySessionCreateRequest request = new PlaySessionCreateRequest(scenarioId);

        int threadCount = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    playSessionService.createSession(userId, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 예외가 DataIntegrityViolationException 이거나 PlayException 이어야 함
                    failCount.incrementAndGet();
                    System.out.println("예상된 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 10개의 스레드가 모두 종료될 때까지 대기

        // then
        List<PlaySession> sessions = playSessionRepository.findAll();

        // 딱 1개만 성공하고, 9개는 튕겨나가야 정상!
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(9);
        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).getActiveKey()).isEqualTo(userId + "_" + scenarioId);
    }
}
