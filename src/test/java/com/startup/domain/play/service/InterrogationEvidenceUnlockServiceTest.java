package com.startup.domain.play.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("C3: 증거 제시 기반 해금 (DoD-4)")
class InterrogationEvidenceUnlockServiceTest {

    @Autowired private InterrogationEvidenceUnlockService unlockService;
    @Autowired private ScenarioRepository scenarioRepository;
    @Autowired private SuspectRepository suspectRepository;
    @Autowired private EvidenceRepository evidenceRepository;
    @Autowired private EvidenceUnlockRuleRepository evidenceUnlockRuleRepository;
    @Autowired private UnlockedEvidenceRepository unlockedEvidenceRepository;
    @Autowired private PlaySessionRepository playSessionRepository;

    private static final Long USER_ID = 8001L;
    private static final String SUS_CARE = "WITNESS_CARE_MANAGER";
    private static final String EV_TRIGGER = "EVIDENCE_TRIGGER_DRAFT";
    private static final String EV_TARGET = "EVIDENCE_TARGET_WEARABLE";

    private Long scenarioId;
    private Long sessionId;
    private Long careManagerId;
    private Long otherSuspectId;
    private Long triggerEvidenceId;
    private Long targetEvidenceId;

    @BeforeEach
    void setUp() {
        Scenario scenario = scenarioRepository.save(Scenario.builder()
                .title("unlock test").description("d")
                .scenarioType(ScenarioType.OFFICIAL).visibility(ScenarioVisibility.PUBLIC)
                .difficulty(Difficulty.NORMAL).status(ScenarioStatus.PUBLISHED).build());
        scenarioId = scenario.getId();

        careManagerId = suspectRepository.save(Suspect.builder()
                .scenarioId(scenarioId).code(SUS_CARE).name("문하연").role("케어매니저")
                .characterType("NEUTRAL_WITNESS").culpritEligible(false)
                .suspicionLevel(0).sortOrder(1).build()).getId();
        otherSuspectId = suspectRepository.save(Suspect.builder()
                .scenarioId(scenarioId).code("SUSPECT_SECRETARY").name("한지오").role("비서실장")
                .characterType("SUSPECT").culpritEligible(true)
                .suspicionLevel(50).sortOrder(2).build()).getId();

        triggerEvidenceId = evidenceRepository.save(Evidence.builder()
                .scenarioId(scenarioId).code(EV_TRIGGER).title("제보 초안").description("d")
                .evidenceType(EvidenceType.DOCUMENT).importance(EvidenceImportance.NORMAL)
                .isInitialPublic(true).unlockType(EvidenceUnlockType.PHASE).sortOrder(1).build()).getId();
        targetEvidenceId = evidenceRepository.save(Evidence.builder()
                .scenarioId(scenarioId).code(EV_TARGET).title("웨어러블 바이탈 원시 데이터").description("d")
                .evidenceType(EvidenceType.DIGITAL_LOG).importance(EvidenceImportance.CORE)
                .isInitialPublic(false).unlockType(EvidenceUnlockType.EVIDENCE_PRESENTED).sortOrder(2).build()).getId();

        evidenceUnlockRuleRepository.save(EvidenceUnlockRule.builder()
                .scenarioId(scenarioId).evidenceId(targetEvidenceId).evidenceCode(EV_TARGET)
                .unlockType("EVIDENCE_PRESENTED")
                .conditionJson("{\"requiredPresentedEvidenceCode\":\"" + EV_TRIGGER
                        + "\",\"requiredCharacterCode\":\"" + SUS_CARE + "\",\"requiredEvidenceCodes\":[]}")
                .sortOrder(10).build());

        sessionId = playSessionRepository.save(PlaySession.builder()
                .userId(USER_ID).scenarioId(scenarioId).build()).getId();
    }

    @AfterEach
    void tearDown() {
        unlockedEvidenceRepository.deleteAllInBatch();
        evidenceUnlockRuleRepository.deleteAllInBatch();
        evidenceRepository.deleteAllInBatch();
        suspectRepository.deleteAllInBatch();
        playSessionRepository.deleteAllInBatch();
        scenarioRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("케어매니저에게 트리거 증거 제시 -> 타깃 증거 해금 + diff 반환")
    void presentingTriggerToCareManager_unlocksTarget() {
        markUnlocked(triggerEvidenceId); // SoT: 제시하려면 트리거가 먼저 해금돼 있어야 함
        List<InterrogationEvidenceUnlockService.UnlockedEvidenceResult> unlocked =
                unlockService.unlockByPresentedEvidence(sessionId, careManagerId, triggerEvidenceId, USER_ID);

        assertThat(unlocked).extracting(InterrogationEvidenceUnlockService.UnlockedEvidenceResult::evidenceId)
                .containsExactly(targetEvidenceId);
        assertThat(unlockedEvidenceRepository.existsByPlaySessionIdAndEvidenceId(sessionId, targetEvidenceId)).isTrue();
    }

    @Test
    @DisplayName("동일 제시 재시도 -> 중복 해금 없음(멱등, diff 비어있음)")
    void repeatedPresentation_isIdempotent() {
        markUnlocked(triggerEvidenceId);
        unlockService.unlockByPresentedEvidence(sessionId, careManagerId, triggerEvidenceId, USER_ID);
        List<InterrogationEvidenceUnlockService.UnlockedEvidenceResult> second =
                unlockService.unlockByPresentedEvidence(sessionId, careManagerId, triggerEvidenceId, USER_ID);

        assertThat(second).isEmpty();
        // 트리거(선해금) + 타깃(1회 해금) = 2건, 재시도로 늘지 않음(멱등)
        assertThat(unlockedEvidenceRepository.findAllByPlaySessionId(sessionId)).hasSize(2);
    }

    @Test
    @DisplayName("다른 용의자에게 같은 증거 제시 -> 해금 안 됨")
    void presentingToWrongSuspect_doesNotUnlock() {
        markUnlocked(triggerEvidenceId);
        List<InterrogationEvidenceUnlockService.UnlockedEvidenceResult> unlocked =
                unlockService.unlockByPresentedEvidence(sessionId, otherSuspectId, triggerEvidenceId, USER_ID);

        assertThat(unlocked).isEmpty();
        assertThat(unlockedEvidenceRepository.existsByPlaySessionIdAndEvidenceId(sessionId, targetEvidenceId)).isFalse();
    }

    @Test
    @DisplayName("관계 없는 증거 제시 -> 해금 안 됨")
    void presentingUnrelatedEvidence_doesNotUnlock() {
        // 타깃 증거 자체를 제시(트리거가 아님) -> 매칭되는 규칙 없음
        markUnlocked(targetEvidenceId); // 게이트가 아니라 규칙 미매칭으로 빈 결과임을 확인
        List<InterrogationEvidenceUnlockService.UnlockedEvidenceResult> unlocked =
                unlockService.unlockByPresentedEvidence(sessionId, careManagerId, targetEvidenceId, USER_ID);

        assertThat(unlocked).isEmpty();
    }

    @Test
    @DisplayName("제시 증거가 미해금이면 -> 해금 안 됨 (SoT: unlocked_evidences 검증)")
    void presentingLockedTrigger_doesNotUnlock() {
        // 트리거를 unlocked_evidences에 넣지 않은 채 제시 -> write-path가 잠긴 증거 제시를 차단
        List<InterrogationEvidenceUnlockService.UnlockedEvidenceResult> unlocked =
                unlockService.unlockByPresentedEvidence(sessionId, careManagerId, triggerEvidenceId, USER_ID);

        assertThat(unlocked).isEmpty();
        assertThat(unlockedEvidenceRepository.existsByPlaySessionIdAndEvidenceId(sessionId, targetEvidenceId)).isFalse();
    }

    private void markUnlocked(Long evidenceId) {
        // 테스트는 트랜잭션 밖이라 @Modifying insertIgnore 대신 save()로 삽입한다.
        unlockedEvidenceRepository.save(UnlockedEvidence.builder()
                .playSessionId(sessionId).evidenceId(evidenceId).unlockedReason("TEST_SETUP").build());
    }
}
