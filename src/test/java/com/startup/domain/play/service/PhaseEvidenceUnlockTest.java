package com.startup.domain.play.service;

import com.startup.domain.play.dto.PlayEvidenceResponse;
import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.entity.UnlockedEvidence;
import com.startup.domain.play.repository.PlaySessionRepository;
import com.startup.domain.play.repository.UnlockedEvidenceRepository;
import com.startup.domain.scenario.entity.Evidence;
import com.startup.domain.scenario.entity.EvidenceUnlockRule;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.Difficulty;
import com.startup.domain.scenario.enums.EvidenceImportance;
import com.startup.domain.scenario.enums.EvidenceType;
import com.startup.domain.scenario.enums.EvidenceUnlockType;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioType;
import com.startup.domain.scenario.enums.ScenarioVisibility;
import com.startup.domain.scenario.repository.EvidenceRepository;
import com.startup.domain.scenario.repository.EvidenceUnlockRuleRepository;
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

    @Autowired
    private EvidenceUnlockRuleRepository evidenceUnlockRuleRepository;

    @AfterEach
    void tearDown() {
        evidenceUnlockRuleRepository.deleteAllInBatch();
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

    @Test
    void getEvidences_waitsForRequiredEvidenceBeforePhaseUnlock() {
        Scenario scenario = scenarioRepository.save(Scenario.builder()
                .title("phase prerequisite test")
                .description("phase unlock prerequisite test")
                .scenarioType(ScenarioType.OFFICIAL)
                .visibility(ScenarioVisibility.PUBLIC)
                .difficulty(Difficulty.NORMAL)
                .status(ScenarioStatus.PUBLISHED)
                .build());

        Evidence prerequisiteEvidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(scenario.getId())
                .code("EVIDENCE_PREREQ")
                .title("prerequisite evidence")
                .description("prerequisite detail")
                .evidenceType(EvidenceType.DOCUMENT)
                .importance(EvidenceImportance.NORMAL)
                .isInitialPublic(false)
                .unlockType(EvidenceUnlockType.NONE)
                .sortOrder(1)
                .build());

        Evidence gatedEvidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(scenario.getId())
                .code("EVIDENCE_GATED")
                .title("gated evidence")
                .description("gated detail")
                .evidenceType(EvidenceType.DOCUMENT)
                .importance(EvidenceImportance.NORMAL)
                .isInitialPublic(false)
                .unlockType(EvidenceUnlockType.PHASE)
                .unlockPhase("PHASE_2_SYSTEM_LOGS")
                .sortOrder(2)
                .build());

        evidenceUnlockRuleRepository.save(EvidenceUnlockRule.builder()
                .scenarioId(scenario.getId())
                .evidenceId(gatedEvidence.getId())
                .evidenceCode(gatedEvidence.getCode())
                .unlockType("PHASE")
                .requiredPhase("PHASE_2_SYSTEM_LOGS")
                .conditionJson("""
                        {"requiredPhase":"PHASE_2_SYSTEM_LOGS","requiredEvidenceCodes":["EVIDENCE_PREREQ"]}
                        """)
                .sortOrder(2)
                .build());

        Long userId = 778L;
        PlaySession session = playSessionRepository.save(PlaySession.builder()
                .userId(userId)
                .scenarioId(scenario.getId())
                .build());
        ReflectionTestUtils.setField(session, "startedAt", LocalDateTime.now().minusMinutes(6));
        playSessionRepository.saveAndFlush(session);

        List<PlayEvidenceResponse> beforePrerequisite =
                playSessionService.getEvidences(userId, session.getId(), false, null);

        assertThat(beforePrerequisite).extracting(PlayEvidenceResponse::evidenceId)
                .doesNotContain(gatedEvidence.getId());
        assertThat(unlockedEvidenceRepository.existsByPlaySessionIdAndEvidenceId(
                session.getId(), gatedEvidence.getId())).isFalse();

        unlockedEvidenceRepository.save(UnlockedEvidence.builder()
                .playSessionId(session.getId())
                .evidenceId(prerequisiteEvidence.getId())
                .unlockedReason("TEST")
                .build());

        List<PlayEvidenceResponse> afterPrerequisite =
                playSessionService.getEvidences(userId, session.getId(), false, null);

        assertThat(afterPrerequisite).extracting(PlayEvidenceResponse::evidenceId)
                .contains(gatedEvidence.getId());
        assertThat(unlockedEvidenceRepository.existsByPlaySessionIdAndEvidenceId(
                session.getId(), gatedEvidence.getId())).isTrue();
    }
}
