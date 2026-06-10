package com.startup.domain.scenario.service;

import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.Difficulty;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioType;
import com.startup.domain.scenario.enums.ScenarioVisibility;
import com.startup.domain.scenario.repository.ScenarioRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScenarioAccessServiceTest {

    private static final long SCENARIO_ID = 10L;
    private static final long CREATOR_ID = 1L;
    private static final long OTHER_USER_ID = 2L;

    private final ScenarioRepository scenarioRepository = mock(ScenarioRepository.class);
    private final ScenarioAccessService scenarioAccessService = new ScenarioAccessService(scenarioRepository);

    @Test
    void canPlayDeniesPublishedPrivateScenarioForNonCreator() {
        when(scenarioRepository.findById(SCENARIO_ID))
                .thenReturn(Optional.of(scenario(ScenarioStatus.PUBLISHED, ScenarioVisibility.PRIVATE)));

        assertThat(scenarioAccessService.canPlay(OTHER_USER_ID, SCENARIO_ID)).isFalse();
    }

    @Test
    void canPlayAllowsPublishedPrivateScenarioForCreator() {
        when(scenarioRepository.findById(SCENARIO_ID))
                .thenReturn(Optional.of(scenario(ScenarioStatus.PUBLISHED, ScenarioVisibility.PRIVATE)));

        assertThat(scenarioAccessService.canPlay(CREATOR_ID, SCENARIO_ID)).isTrue();
    }

    @Test
    void canViewDeniesPublishedPrivateScenarioForAnonymous() {
        when(scenarioRepository.findById(SCENARIO_ID))
                .thenReturn(Optional.of(scenario(ScenarioStatus.PUBLISHED, ScenarioVisibility.PRIVATE)));

        assertThat(scenarioAccessService.canView(null, SCENARIO_ID)).isFalse();
    }

    @Test
    void canViewAllowsPublishedUnlistedScenarioForAnonymous() {
        when(scenarioRepository.findById(SCENARIO_ID))
                .thenReturn(Optional.of(scenario(ScenarioStatus.PUBLISHED, ScenarioVisibility.UNLISTED)));

        assertThat(scenarioAccessService.canView(null, SCENARIO_ID)).isTrue();
    }

    private Scenario scenario(ScenarioStatus status, ScenarioVisibility visibility) {
        return Scenario.builder()
                .title("access scenario")
                .description("access scenario")
                .scenarioType(ScenarioType.CUSTOM)
                .visibility(visibility)
                .difficulty(Difficulty.NORMAL)
                .creatorId(CREATOR_ID)
                .status(status)
                .build();
    }
}
