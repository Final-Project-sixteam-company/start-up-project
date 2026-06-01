package com.startup.domain.play.service;

import com.startup.domain.play.dto.PlayEvidenceResponse;
import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.repository.PlaySessionRepository;
import com.startup.domain.play.repository.UnlockedEvidenceRepository;
import com.startup.domain.scenario.entity.Evidence;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.Difficulty;
import com.startup.domain.scenario.enums.EvidenceImportance;
import com.startup.domain.scenario.enums.EvidenceType;
import com.startup.domain.scenario.enums.EvidenceUnlockType;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioType;
import com.startup.domain.scenario.enums.ScenarioVisibility;
import com.startup.domain.scenario.repository.EvidenceRepository;
import com.startup.domain.scenario.repository.ScenarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PhaseEvidenceUnlockTest {

    @Autowired
    private PlaySessionService playSessionService;

    @Autowired
    private PlaySessionRepository playSessionRepository;

    @Autowired
    private UnlockedEvidenceRepository unlockedEvidenceRepository;

    @Autowired
    private ScenarioRepository scenarioRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @AfterEach
    void tearDown() {
        unlockedEvidenceRepository.deleteAllInBatch();
        evidenceRepository.deleteAllInBatch();
        playSessionRepository.deleteAllInBatch();
        scenarioRepository.deleteAllInBatch();
    }

    @Test
    void getEvidences_autoUnlocksPhaseEvidenceByElapsedTime() {
        Scenario scenario = scenarioRepository.save(Scenario.builder()
                .title("phase test")
                .description("phase unlock test")
                .scenarioType(ScenarioType.OFFICIAL)
                .visibility(ScenarioVisibility.PUBLIC)
                .difficulty(Difficulty.NORMAL)
                .status(ScenarioStatus.PUBLISHED)
                .build());

        Evidence phaseEvidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(scenario.getId())
                .code("EVIDENCE_PHASE_2")
                .title("phase evidence")
                .description("phase evidence detail")
                .evidenceType(EvidenceType.DOCUMENT)
                .importance(EvidenceImportance.NORMAL)
                .isInitialPublic(false)
                .unlockType(EvidenceUnlockType.PHASE)
                .unlockPhase("PHASE_2_SYSTEM_LOGS")
                .sortOrder(1)
                .build());

        Long userId = 777L;
        PlaySession session = playSessionRepository.save(PlaySession.builder()
                .userId(userId)
                .scenarioId(scenario.getId())
                .build());
        ReflectionTestUtils.setField(session, "startedAt", LocalDateTime.now().minusMinutes(6));
        playSessionRepository.saveAndFlush(session);

        List<PlayEvidenceResponse> evidences =
                playSessionService.getEvidences(userId, session.getId(), false, null);

        assertThat(evidences).extracting(PlayEvidenceResponse::evidenceId)
                .containsExactly(phaseEvidence.getId());
        assertThat(unlockedEvidenceRepository.existsByPlaySessionIdAndEvidenceId(
                session.getId(), phaseEvidence.getId())).isTrue();
    }
}
