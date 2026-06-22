package com.startup.domain.play.service;

import com.startup.domain.ai.repository.InterrogationLogRepository;
import com.startup.domain.play.repository.PlaySessionRepository;
import com.startup.domain.play.repository.UnlockedEvidenceRepository;
import com.startup.domain.play.repository.UsedHintRepository;
import com.startup.domain.play.support.ActivePlaySessionLookup;
import com.startup.domain.play.support.EvidenceUnlockPolicy;
import com.startup.domain.play.support.EvidenceVariantDescriptionResolver;
import com.startup.domain.play.support.FinalDeductionLockManager;
import com.startup.domain.scenario.repository.EvidenceRepository;
import com.startup.domain.scenario.repository.EvidenceSuspectRepository;
import com.startup.domain.scenario.repository.EvidenceUnlockRuleRepository;
import com.startup.domain.scenario.repository.HintRepository;
import com.startup.domain.scenario.repository.ScenarioLocationRepository;
import com.startup.domain.scenario.repository.ScenarioRepository;
import com.startup.domain.scenario.repository.ScenarioVariantRepository;
import com.startup.domain.scenario.repository.SuspectRepository;
import com.startup.domain.scenario.repository.TimelineEventRepository;
import com.startup.domain.scenario.repository.VictimRepository;
import com.startup.domain.scenario.support.ScenarioAssetUrlResolver;
import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.entity.UnlockedEvidence;
import com.startup.domain.scenario.entity.Evidence;
import com.startup.domain.scenario.enums.EvidenceImportance;
import com.startup.domain.scenario.enums.EvidenceType;
import com.startup.domain.scenario.enums.EvidenceUnlockType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaySessionUnlockIdempotencyTest {

    @InjectMocks
    private PlaySessionService playSessionService;

    @Mock
    private PlaySessionRepository playSessionRepository;
    @Mock
    private UnlockedEvidenceRepository unlockedEvidenceRepository;
    @Mock
    private ScenarioRepository scenarioRepository;
    @Mock
    private com.startup.domain.scenario.service.ScenarioAccessService scenarioAccessService;
    @Mock
    private EvidenceRepository evidenceRepository;
    @Mock
    private SuspectRepository suspectRepository;
    @Mock
    private VictimRepository victimRepository;
    @Mock
    private ScenarioLocationRepository scenarioLocationRepository;
    @Mock
    private EvidenceSuspectRepository evidenceSuspectRepository;
    @Mock
    private EvidenceUnlockRuleRepository evidenceUnlockRuleRepository;
    @Mock
    private HintRepository hintRepository;
    @Mock
    private UsedHintRepository usedHintRepository;
    @Mock
    private InterrogationLogRepository interrogationLogRepository;
    @Mock
    private ScenarioVariantRepository scenarioVariantRepository;
    @Mock
    private ActivePlaySessionLookup activePlaySessionLookup;
    @Mock
    private FinalDeductionLockManager finalDeductionLockManager;
    @Mock
    private EvidenceVariantDescriptionResolver evidenceVariantDescriptionResolver;
    @Mock
    private EvidenceUnlockPolicy evidenceUnlockPolicy;
    @Mock
    private ScenarioAssetUrlResolver scenarioAssetUrlResolver;
    @Mock
    private TimelineEventRepository timelineEventRepository;

    @Test
    void unlockEvidence_whenInsertIgnoreLosesToConcurrentRequest_usesLockingReadAndReturnsExistingUnlock() {
        PlaySession session = PlaySession.builder()
                .userId(1L)
                .scenarioId(10L)
                .build();
        ReflectionTestUtils.setField(session, "id", 100L);

        Evidence evidence = Evidence.builder()
                .scenarioId(10L)
                .title("manual evidence")
                .description("manual evidence detail")
                .evidenceType(EvidenceType.DOCUMENT)
                .importance(EvidenceImportance.NORMAL)
                .isInitialPublic(false)
                .unlockType(EvidenceUnlockType.MANUAL)
                .sortOrder(1)
                .build();
        ReflectionTestUtils.setField(evidence, "id", 200L);

        UnlockedEvidence existingUnlock = UnlockedEvidence.builder()
                .playSessionId(100L)
                .evidenceId(200L)
                .unlockedReason("MANUAL")
                .build();

        when(playSessionRepository.findById(100L)).thenReturn(Optional.of(session));
        when(finalDeductionLockManager.isLocked(100L)).thenReturn(false);
        when(evidenceRepository.findById(200L)).thenReturn(Optional.of(evidence));
        when(unlockedEvidenceRepository.findByPlaySessionIdAndEvidenceId(100L, 200L))
                .thenReturn(Optional.empty());
        when(unlockedEvidenceRepository.insertIgnoreUnlockedEvidence(100L, 200L, "MANUAL"))
                .thenReturn(0);
        when(unlockedEvidenceRepository.findByPlaySessionIdAndEvidenceIdForUpdate(100L, 200L))
                .thenReturn(Optional.of(existingUnlock));

        var response = playSessionService.unlockEvidence(1L, 100L, 200L, null);

        assertThat(response.evidenceId()).isEqualTo(200L);
        assertThat(response.isUnlocked()).isTrue();
        assertThat(response.unlockedAt()).isEqualTo(existingUnlock.getUnlockedAt());
        verify(unlockedEvidenceRepository).findByPlaySessionIdAndEvidenceIdForUpdate(100L, 200L);
    }
}
