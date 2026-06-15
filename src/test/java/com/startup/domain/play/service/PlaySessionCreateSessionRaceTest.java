package com.startup.domain.play.service;

import com.startup.domain.ai.repository.InterrogationLogRepository;
import com.startup.domain.play.dto.ActivePlaySessionResponse;
import com.startup.domain.play.dto.PlaySessionCreateRequest;
import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.enums.PlaySessionStatus;
import com.startup.domain.play.error.PlayErrorCode;
import com.startup.domain.play.error.PlayException;
import com.startup.domain.play.repository.PlaySessionRepository;
import com.startup.domain.play.repository.UnlockedEvidenceRepository;
import com.startup.domain.play.repository.UsedHintRepository;
import com.startup.domain.play.support.EvidenceUnlockPolicy;
import com.startup.domain.play.support.EvidenceVariantDescriptionResolver;
import com.startup.domain.play.support.FinalDeductionLockManager;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.Difficulty;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioType;
import com.startup.domain.scenario.enums.ScenarioVisibility;
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
import com.startup.domain.scenario.service.ScenarioAccessService;
import com.startup.domain.scenario.support.ScenarioAssetUrlResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class PlaySessionCreateSessionRaceTest {

    private static final Long USER_ID = 1L;
    private static final Long SCENARIO_ID = 10L;

    @InjectMocks
    private PlaySessionService playSessionService;

    @Mock
    private PlaySessionRepository playSessionRepository;
    @Mock
    private UnlockedEvidenceRepository unlockedEvidenceRepository;
    @Mock
    private ScenarioRepository scenarioRepository;
    @Mock
    private ScenarioAccessService scenarioAccessService;
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
    void createSession_whenDuplicateSaveFindsActiveSession_throwsP002WithActiveSessionIdOnly() {
        PlaySession existing = playingSession(100L);
        when(scenarioRepository.findByIdForUpdate(SCENARIO_ID)).thenReturn(Optional.of(scenario()));
        when(playSessionRepository.findByUserIdAndScenarioIdAndStatus(USER_ID, SCENARIO_ID, PlaySessionStatus.PLAYING))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(scenarioVariantRepository.findAllByScenarioIdAndIsActiveTrueOrderBySortOrderAsc(SCENARIO_ID))
                .thenReturn(List.of());
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(playSessionRepository).saveAndFlush(any(PlaySession.class));

        Throwable thrown = catchThrowable(
                () -> playSessionService.createSession(USER_ID, new PlaySessionCreateRequest(SCENARIO_ID))
        );

        assertThat(thrown).isInstanceOfSatisfying(PlayException.class, exception -> {
            assertThat(exception.getErrorCode()).isEqualTo(PlayErrorCode.SESSION_ALREADY_EXISTS);
            assertThat(exception.getDetails()).containsOnly(Map.entry("activeSessionId", 100L));
        });
        verify(scenarioRepository, never()).incrementPlayCount(SCENARIO_ID);
    }

    @Test
    void createSession_whenDuplicateLookupFails_fallsBackToP002WithoutDetails() {
        when(scenarioRepository.findByIdForUpdate(SCENARIO_ID)).thenReturn(Optional.of(scenario()));
        when(playSessionRepository.findByUserIdAndScenarioIdAndStatus(USER_ID, SCENARIO_ID, PlaySessionStatus.PLAYING))
                .thenReturn(Optional.empty())
                .thenThrow(new IllegalStateException("rollback-only"));
        when(scenarioVariantRepository.findAllByScenarioIdAndIsActiveTrueOrderBySortOrderAsc(SCENARIO_ID))
                .thenReturn(List.of());
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(playSessionRepository).saveAndFlush(any(PlaySession.class));

        Throwable thrown = catchThrowable(
                () -> playSessionService.createSession(USER_ID, new PlaySessionCreateRequest(SCENARIO_ID))
        );

        assertThat(thrown).isInstanceOfSatisfying(PlayException.class, exception -> {
            assertThat(exception.getErrorCode()).isEqualTo(PlayErrorCode.SESSION_ALREADY_EXISTS);
            assertThat(exception.getDetails()).isNull();
        });
        verify(scenarioRepository, never()).incrementPlayCount(SCENARIO_ID);
    }

    @Test
    void getActiveSession_validatesPlayableBeforeLookup() {
        when(playSessionRepository.findByUserIdAndScenarioIdAndStatus(USER_ID, SCENARIO_ID, PlaySessionStatus.PLAYING))
                .thenReturn(Optional.empty());

        ActivePlaySessionResponse response = playSessionService.getActiveSession(USER_ID, SCENARIO_ID);

        assertThat(response.hasActiveSession()).isFalse();
        assertThat(response.scenarioId()).isEqualTo(SCENARIO_ID);
        verify(scenarioAccessService).validatePlayable(USER_ID, SCENARIO_ID);
        verify(playSessionRepository).findByUserIdAndScenarioIdAndStatus(USER_ID, SCENARIO_ID, PlaySessionStatus.PLAYING);
    }

    private Scenario scenario() {
        return Scenario.builder()
                .title("race-test")
                .description("race test scenario")
                .synopsis("race test synopsis")
                .scenarioType(ScenarioType.OFFICIAL)
                .visibility(ScenarioVisibility.PUBLIC)
                .difficulty(Difficulty.NORMAL)
                .estimatedPlayTimeMinutes(30)
                .playerCountMin(1)
                .playerCountMax(1)
                .status(ScenarioStatus.PUBLISHED)
                .build();
    }

    private PlaySession playingSession(Long sessionId) {
        PlaySession session = PlaySession.builder()
                .userId(USER_ID)
                .scenarioId(SCENARIO_ID)
                .build();
        ReflectionTestUtils.setField(session, "id", sessionId);
        return session;
    }
}
