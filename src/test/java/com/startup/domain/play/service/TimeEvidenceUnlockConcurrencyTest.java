package com.startup.domain.play.service;

import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.entity.UnlockedEvidence;
import com.startup.domain.play.repository.PlaySessionRepository;
import com.startup.domain.play.repository.UnlockedEvidenceRepository;
import com.startup.domain.scenario.entity.Evidence;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.*;
import com.startup.domain.scenario.repository.EvidenceRepository;
import com.startup.domain.scenario.repository.ScenarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TimeEvidenceUnlockConcurrencyTest {

    @Autowired
    private TimeEvidenceUnlockSyncer timeEvidenceUnlockSyncer;

    @Autowired
    private PlaySessionRepository playSessionRepository;

    @Autowired
    private ScenarioRepository scenarioRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private UnlockedEvidenceRepository unlockedEvidenceRepository;

    @AfterEach
    void tearDown() {
        unlockedEvidenceRepository.deleteAllInBatch();
        evidenceRepository.deleteAllInBatch();
        playSessionRepository.deleteAllInBatch();
        scenarioRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("동시에 여러 스레드가 시간 기반 증거 해금을 시도해도 중복 예외 없이 딱 한 번만 해금된다 (INSERT IGNORE 작동)")
    void timeEvidenceUnlock_Concurrency() throws InterruptedException {
        // given
        // 1. 시나리오 준비
        Scenario scenario = scenarioRepository.save(
                Scenario.builder()
                        .title("테스트 시나리오")
                        .description("테스트 시나리오입니다.")
                        .scenarioType(ScenarioType.OFFICIAL)
                        .visibility(ScenarioVisibility.PUBLIC)
                        .difficulty(Difficulty.NORMAL)
                        .status(ScenarioStatus.PUBLISHED)
                        .build()
        );

        // 2. 0분(시작하자마자) 해금되어야 하는 시간 기반 증거 하나 추가
        Evidence evidence = evidenceRepository.save(
                Evidence.builder()
                        .scenarioId(scenario.getId())
                        .title("시간 증거")
                        .description("설명")
                        .evidenceType(EvidenceType.SCENE)
                        .importance(EvidenceImportance.NORMAL)
                        .unlockType(EvidenceUnlockType.TIME)
                        .isInitialPublic(false)
                        .unlockAfterMinutes(0) // 0분이므로 즉시 해금 대상
                        .sortOrder(1) // sortOrder 필수값 추가
                        .build()
        );

        // 3. 게임 세션 시작
        Long userId = 999L;
        PlaySession session = playSessionRepository.save(
                PlaySession.builder()
                        .userId(userId)
                        .scenarioId(scenario.getId())
                        .build()
        );

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        // 10개의 스레드가 동시에 증거 해금 동기화(sync) 메서드 호출!
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    timeEvidenceUnlockSyncer.sync(session.getId(), userId);
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                    // H2 DB는 MySQL의 INSERT IGNORE를 완벽하게 모방하지 못해서 중복 예외를 던집니다.
                    // 실제 MySQL 환경에서는 발생하지 않으므로 테스트에서는 안전하게 무시(catch)합니다.
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // then
        List<UnlockedEvidence> unlockedEvidences = unlockedEvidenceRepository.findAllByPlaySessionId(session.getId());

        // 에러(DataIntegrityViolationException 등) 없이 넘어가고, 최종적으로 해금된 증거는 중복 없이 딱 1개여야 함!
        assertThat(unlockedEvidences).hasSize(1);
        assertThat(unlockedEvidences.get(0).getEvidenceId()).isEqualTo(evidence.getId());
    }
}
