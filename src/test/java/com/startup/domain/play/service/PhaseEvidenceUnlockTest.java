package com.startup.domain.play.service;

import com.startup.domain.play.dto.PlayEvidenceResponse;
import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.entity.UnlockedEvidence;
import com.startup.domain.play.repository.PlaySessionRepository;
import com.startup.domain.play.repository.UnlockedEvidenceRepository;
import com.startup.domain.scenario.entity.Evidence;
import com.startup.domain.scenario.entity.EvidenceUnlockRule;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.entity.Suspect;
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
import com.startup.domain.scenario.repository.SuspectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "clueroom.assets.public-base-url=https://assets.example.com")
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

    @Autowired
    private SuspectRepository suspectRepository;

    @AfterEach
    void tearDown() {
        evidenceUnlockRuleRepository.deleteAllInBatch();
        unlockedEvidenceRepository.deleteAllInBatch();
        suspectRepository.deleteAllInBatch();
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

    @Test
    void getEvidences_resolvesImageUrlForUnlockedEvidenceAssetKeyWithoutExposingAssetKey() {
        Scenario scenario = scenarioRepository.save(Scenario.builder()
                .title("image test")
                .description("image url test")
                .scenarioType(ScenarioType.OFFICIAL)
                .visibility(ScenarioVisibility.PUBLIC)
                .difficulty(Difficulty.NORMAL)
                .status(ScenarioStatus.PUBLISHED)
                .build());

        Evidence evidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(scenario.getId())
                .code("EVIDENCE_IMAGE")
                .title("image evidence")
                .description("image evidence detail")
                .evidenceType(EvidenceType.VISUAL)
                .importance(EvidenceImportance.NORMAL)
                .imageAssetKey("official/seowolchae/v1/evidence/EVIDENCE_IMAGE.png")
                .isInitialPublic(false)
                .unlockType(EvidenceUnlockType.NONE)
                .sortOrder(1)
                .build());

        Long userId = 779L;
        PlaySession session = playSessionRepository.save(PlaySession.builder()
                .userId(userId)
                .scenarioId(scenario.getId())
                .build());
        unlockedEvidenceRepository.save(UnlockedEvidence.builder()
                .playSessionId(session.getId())
                .evidenceId(evidence.getId())
                .unlockedReason("TEST")
                .build());

        List<PlayEvidenceResponse> evidences =
                playSessionService.getEvidences(userId, session.getId(), false, null);

        assertThat(evidences).hasSize(1);
        assertThat(evidences.getFirst().imageUrl())
                .isEqualTo("https://assets.example.com/official/seowolchae/v1/evidence/EVIDENCE_IMAGE.png");
    }

    @Test
    void getEvidences_keepsLockedEvidenceImageMasked() {
        Scenario scenario = scenarioRepository.save(Scenario.builder()
                .title("locked image test")
                .description("locked image mask test")
                .scenarioType(ScenarioType.OFFICIAL)
                .visibility(ScenarioVisibility.PUBLIC)
                .difficulty(Difficulty.NORMAL)
                .status(ScenarioStatus.PUBLISHED)
                .build());

        Evidence evidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(scenario.getId())
                .code("EVIDENCE_LOCKED_IMAGE")
                .title("locked image evidence")
                .description("locked image evidence detail")
                .evidenceType(EvidenceType.VISUAL)
                .importance(EvidenceImportance.NORMAL)
                .imageAssetKey("official/seowolchae/v1/evidence/EVIDENCE_LOCKED_IMAGE.png")
                .isInitialPublic(false)
                .unlockType(EvidenceUnlockType.NONE)
                .sortOrder(1)
                .build());

        Long userId = 780L;
        PlaySession session = playSessionRepository.save(PlaySession.builder()
                .userId(userId)
                .scenarioId(scenario.getId())
                .build());

        List<PlayEvidenceResponse> evidences =
                playSessionService.getEvidences(userId, session.getId(), true, null);

        assertThat(evidences).hasSize(1);
        assertThat(evidences.getFirst().evidenceId()).isEqualTo(evidence.getId());
        assertThat(evidences.getFirst().isUnlocked()).isFalse();
        assertThat(evidences.getFirst().imageUrl()).isNull();
    }

    @Test
    void getSuspects_resolvesPortraitImageUrlFromAssetKey() {
        Scenario scenario = scenarioRepository.save(Scenario.builder()
                .title("suspect image test")
                .description("suspect portrait test")
                .scenarioType(ScenarioType.OFFICIAL)
                .visibility(ScenarioVisibility.PUBLIC)
                .difficulty(Difficulty.NORMAL)
                .status(ScenarioStatus.PUBLISHED)
                .build());

        Suspect suspect = suspectRepository.save(Suspect.builder()
                .scenarioId(scenario.getId())
                .code("SUSPECT_TEST")
                .name("suspect")
                .role("role")
                .publicStatement("statement")
                .alibi("alibi")
                .portraitAssetKey("official/seowolchae/v1/characters/character-001.png")
                .suspicionLevel(50)
                .sortOrder(1)
                .build());

        Long userId = 781L;
        PlaySession session = playSessionRepository.save(PlaySession.builder()
                .userId(userId)
                .scenarioId(scenario.getId())
                .build());

        var suspects = playSessionService.getSuspects(userId, session.getId());

        assertThat(suspects).hasSize(1);
        assertThat(suspects.getFirst().suspectId()).isEqualTo(suspect.getId());
        assertThat(suspects.getFirst().portraitImageUrl())
                .isEqualTo("https://assets.example.com/official/seowolchae/v1/characters/character-001.png");
    }
}
