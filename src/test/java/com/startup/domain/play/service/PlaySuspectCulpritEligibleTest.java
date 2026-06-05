package com.startup.domain.play.service;

import com.startup.domain.play.dto.PlaySuspectResponse;
import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.repository.PlaySessionRepository;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.entity.Suspect;
import com.startup.domain.scenario.enums.Difficulty;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioType;
import com.startup.domain.scenario.enums.ScenarioVisibility;
import com.startup.domain.scenario.repository.ScenarioRepository;
import com.startup.domain.scenario.repository.SuspectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("C2-BE: 용의자 목록 응답에 culpritEligible이 노출된다 (P1-4)")
class PlaySuspectCulpritEligibleTest {

    @Autowired
    private PlaySessionService playSessionService;

    @Autowired
    private ScenarioRepository scenarioRepository;

    @Autowired
    private SuspectRepository suspectRepository;

    @Autowired
    private PlaySessionRepository playSessionRepository;

    private static final Long USER_ID = 7001L;

    @AfterEach
    void tearDown() {
        suspectRepository.deleteAllInBatch();
        playSessionRepository.deleteAllInBatch();
        scenarioRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("지목 가능 용의자는 true, 지목 불가(증인)는 false로 내려간다")
    void getSuspects_exposesCulpritEligible() {
        Scenario scenario = scenarioRepository.save(Scenario.builder()
                .title("culpritEligible test")
                .description("desc")
                .scenarioType(ScenarioType.OFFICIAL)
                .visibility(ScenarioVisibility.PUBLIC)
                .difficulty(Difficulty.NORMAL)
                .status(ScenarioStatus.PUBLISHED)
                .build());

        suspectRepository.save(Suspect.builder()
                .scenarioId(scenario.getId())
                .name("범인후보")
                .role("비서실장")
                .characterType("SUSPECT")
                .culpritEligible(true)
                .suspicionLevel(50)
                .sortOrder(1)
                .build());
        suspectRepository.save(Suspect.builder()
                .scenarioId(scenario.getId())
                .name("증인")
                .role("케어매니저")
                .characterType("NEUTRAL_WITNESS")
                .culpritEligible(false)
                .suspicionLevel(0)
                .sortOrder(2)
                .build());

        PlaySession session = playSessionRepository.save(PlaySession.builder()
                .userId(USER_ID)
                .scenarioId(scenario.getId())
                .build());

        List<PlaySuspectResponse> suspects = playSessionService.getSuspects(USER_ID, session.getId());

        Map<String, PlaySuspectResponse> byName = suspects.stream()
                .collect(Collectors.toMap(PlaySuspectResponse::name, Function.identity()));

        assertThat(byName.get("범인후보").culpritEligible()).isTrue();
        assertThat(byName.get("증인").culpritEligible()).isFalse();
    }
}
