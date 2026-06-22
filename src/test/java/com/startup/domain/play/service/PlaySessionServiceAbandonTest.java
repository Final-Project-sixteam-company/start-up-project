package com.startup.domain.play.service;

import com.startup.domain.play.dto.ActivePlaySessionResponse;
import com.startup.domain.play.dto.PlaySessionCreateRequest;
import com.startup.domain.play.dto.PlaySessionCreateResponse;
import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.enums.PlaySessionStatus;
import com.startup.domain.play.error.PlayErrorCode;
import com.startup.domain.play.error.PlayException;
import com.startup.domain.play.repository.PlaySessionRepository;
import com.startup.domain.play.support.FinalDeductionLockManager;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.Difficulty;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioType;
import com.startup.domain.scenario.enums.ScenarioVisibility;
import com.startup.domain.scenario.repository.ScenarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlaySessionServiceAbandonTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Autowired
    private PlaySessionService playSessionService;

    @Autowired
    private PlaySessionRepository playSessionRepository;

    @Autowired
    private ScenarioRepository scenarioRepository;

    @Autowired
    private FinalDeductionLockManager finalDeductionLockManager;

    @Autowired
    private EntityManager entityManager;

    @Test
    void abandonSession_withPlayingSession_marksAbandonedAndReleasesActiveKey() {
        Scenario scenario = saveScenario();
        PlaySession session = savePlayingSession(USER_ID, scenario.getId());

        playSessionService.abandonSession(USER_ID, session.getId());
        flushAndClear();

        PlaySession found = playSessionRepository.findById(session.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(PlaySessionStatus.ABANDONED);
        assertThat(found.getActiveKey()).isNull();
        assertThat(found.getEndedAt()).isNotNull();
    }

    @Test
    void createSession_afterAbandon_allowsSameUserAndScenarioAgain() {
        Scenario scenario = saveScenario();
        PlaySessionCreateResponse first = playSessionService.createSession(
                USER_ID,
                new PlaySessionCreateRequest(scenario.getId())
        );

        playSessionService.abandonSession(USER_ID, first.sessionId());
        flushAndClear();

        PlaySessionCreateResponse second = playSessionService.createSession(
                USER_ID,
                new PlaySessionCreateRequest(scenario.getId())
        );
        flushAndClear();

        PlaySession newSession = playSessionRepository.findById(second.sessionId()).orElseThrow();
        assertThat(second.sessionId()).isNotEqualTo(first.sessionId());
        assertThat(newSession.getStatus()).isEqualTo(PlaySessionStatus.PLAYING);
        assertThat(newSession.getActiveKey()).isEqualTo(USER_ID + "_" + scenario.getId());
    }

    @Test
    void createSession_withExistingPlayingSession_throwsP002WithActiveSessionDetails() {
        Scenario scenario = saveScenario();
        PlaySession existing = savePlayingSession(USER_ID, scenario.getId());

        Throwable thrown = catchThrowable(
                () -> playSessionService.createSession(USER_ID, new PlaySessionCreateRequest(scenario.getId()))
        );

        assertThat(thrown).isInstanceOfSatisfying(PlayException.class, exception -> {
            assertThat(exception.getErrorCode()).isEqualTo(PlayErrorCode.SESSION_ALREADY_EXISTS);
            assertThat(exception.getDetails()).containsOnly(Map.entry("activeSessionId", existing.getId()));
        });
    }

    @Test
    void getActiveSession_withPlayingSession_returnsActiveSession() {
        Scenario scenario = saveScenario();
        PlaySession session = savePlayingSession(USER_ID, scenario.getId());

        ActivePlaySessionResponse response = playSessionService.getActiveSession(USER_ID, scenario.getId());

        assertThat(response.hasActiveSession()).isTrue();
        assertThat(response.activeSessionId()).isEqualTo(session.getId());
        assertThat(response.scenarioId()).isEqualTo(scenario.getId());
        assertThat(response.status()).isEqualTo(PlaySessionStatus.PLAYING);
        assertThat(response.startedAt()).isEqualTo(session.getStartedAt());
    }

    @Test
    void getActiveSession_withOtherUserPlayingSession_returnsNone() {
        Scenario scenario = saveScenario();
        savePlayingSession(OTHER_USER_ID, scenario.getId());

        ActivePlaySessionResponse response = playSessionService.getActiveSession(USER_ID, scenario.getId());

        assertThat(response.hasActiveSession()).isFalse();
        assertThat(response.activeSessionId()).isNull();
        assertThat(response.scenarioId()).isEqualTo(scenario.getId());
        assertThat(response.status()).isNull();
        assertThat(response.startedAt()).isNull();
    }

    @Test
    void getActiveSession_withoutPlayingSession_returnsNone() {
        Scenario scenario = saveScenario();
        PlaySession abandoned = savePlayingSession(USER_ID, scenario.getId());
        abandoned.abandon();
        flushAndClear();

        ActivePlaySessionResponse response = playSessionService.getActiveSession(USER_ID, scenario.getId());

        assertThat(response.hasActiveSession()).isFalse();
        assertThat(response.activeSessionId()).isNull();
        assertThat(response.scenarioId()).isEqualTo(scenario.getId());
        assertThat(response.status()).isNull();
        assertThat(response.startedAt()).isNull();
    }

    @Test
    void getActiveSession_withCompletedSession_returnsNone() {
        Scenario scenario = saveScenario();
        PlaySession completed = savePlayingSession(USER_ID, scenario.getId());
        completed.markCompleted();
        flushAndClear();

        ActivePlaySessionResponse response = playSessionService.getActiveSession(USER_ID, scenario.getId());

        assertThat(response.hasActiveSession()).isFalse();
        assertThat(response.activeSessionId()).isNull();
        assertThat(response.scenarioId()).isEqualTo(scenario.getId());
        assertThat(response.status()).isNull();
        assertThat(response.startedAt()).isNull();
    }

    @Test
    void abandonSession_withCompletedSession_throwsNotPlayingAndKeepsCompletedStatus() {
        Scenario scenario = saveScenario();
        PlaySession session = savePlayingSession(USER_ID, scenario.getId());
        session.markCompleted();
        flushAndClear();

        assertPlayError(
                () -> playSessionService.abandonSession(USER_ID, session.getId()),
                PlayErrorCode.SESSION_NOT_PLAYING
        );

        PlaySession found = playSessionRepository.findById(session.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(PlaySessionStatus.COMPLETED);
        assertThat(found.getActiveKey()).isNull();
    }

    @Test
    void abandonSession_whileFinalDeductionInFlight_throwsNotPlayingAndKeepsPlayingStatus() {
        Scenario scenario = saveScenario();
        PlaySession session = savePlayingSession(USER_ID, scenario.getId());
        assertThat(finalDeductionLockManager.tryLock(session.getId())).isTrue();

        try {
            assertPlayError(
                    () -> playSessionService.abandonSession(USER_ID, session.getId()),
                    PlayErrorCode.SESSION_NOT_PLAYING
            );
        } finally {
            finalDeductionLockManager.release(session.getId());
        }

        flushAndClear();
        PlaySession found = playSessionRepository.findById(session.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(PlaySessionStatus.PLAYING);
        assertThat(found.getActiveKey()).isEqualTo(USER_ID + "_" + scenario.getId());
    }

    @Test
    void abandonSession_withAlreadyAbandonedSession_throwsNotPlaying() {
        Scenario scenario = saveScenario();
        PlaySession session = savePlayingSession(USER_ID, scenario.getId());
        playSessionService.abandonSession(USER_ID, session.getId());
        flushAndClear();

        assertPlayError(
                () -> playSessionService.abandonSession(USER_ID, session.getId()),
                PlayErrorCode.SESSION_NOT_PLAYING
        );
    }

    @Test
    void completeSession_withAbandonedSession_throwsNotPlayingAndKeepsAbandonedStatus() {
        Scenario scenario = saveScenario();
        PlaySession session = savePlayingSession(USER_ID, scenario.getId());
        playSessionService.abandonSession(USER_ID, session.getId());
        flushAndClear();

        assertPlayError(
                () -> playSessionService.completeSession(session.getId()),
                PlayErrorCode.SESSION_NOT_PLAYING
        );

        PlaySession found = playSessionRepository.findById(session.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(PlaySessionStatus.ABANDONED);
        assertThat(found.getActiveKey()).isNull();
    }

    @Test
    void abandonSession_withDifferentOwner_throwsAccessDenied() {
        Scenario scenario = saveScenario();
        PlaySession session = savePlayingSession(OTHER_USER_ID, scenario.getId());

        assertPlayError(
                () -> playSessionService.abandonSession(USER_ID, session.getId()),
                PlayErrorCode.SESSION_ACCESS_DENIED
        );

        PlaySession found = playSessionRepository.findById(session.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(PlaySessionStatus.PLAYING);
        assertThat(found.getActiveKey()).isEqualTo(OTHER_USER_ID + "_" + scenario.getId());
    }

    @Test
    void abandonSession_withMissingSession_throwsNotFound() {
        assertPlayError(
                () -> playSessionService.abandonSession(USER_ID, 999_999L),
                PlayErrorCode.SESSION_NOT_FOUND
        );
    }

    @Test
    void abandonSession_withDifferentOwnerNonPlayingSession_prioritizesAccessDenied() {
        Scenario scenario = saveScenario();
        PlaySession session = savePlayingSession(OTHER_USER_ID, scenario.getId());
        session.markCompleted();
        flushAndClear();

        assertPlayError(
                () -> playSessionService.abandonSession(USER_ID, session.getId()),
                PlayErrorCode.SESSION_ACCESS_DENIED
        );
    }

    private PlaySession savePlayingSession(Long userId, Long scenarioId) {
        PlaySession session = PlaySession.builder()
                .userId(userId)
                .scenarioId(scenarioId)
                .build();
        return playSessionRepository.saveAndFlush(session);
    }

    private Scenario saveScenario() {
        Scenario scenario = Scenario.builder()
                .title("abandon-test")
                .description("abandon test scenario")
                .synopsis("abandon test synopsis")
                .scenarioType(ScenarioType.OFFICIAL)
                .visibility(ScenarioVisibility.PUBLIC)
                .difficulty(Difficulty.NORMAL)
                .estimatedPlayTimeMinutes(30)
                .playerCountMin(1)
                .playerCountMax(1)
                .status(ScenarioStatus.PUBLISHED)
                .build();
        return scenarioRepository.saveAndFlush(scenario);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private void assertPlayError(Runnable action, PlayErrorCode expectedErrorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(PlayException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode));
    }
}
