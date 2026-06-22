package com.startup.domain.play.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("C2-BE: 용의자 목록 응답은 정답성 metadata를 노출하지 않는다")
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        suspectRepository.deleteAllInBatch();
        playSessionRepository.deleteAllInBatch();
        scenarioRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("culpritEligible과 suspicionLevel은 public suspect response에 포함하지 않는다")
    void getSuspects_omitsTruthAdjacentMetadata() throws JsonProcessingException {
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
                .name("후보A")
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

        assertThat(suspects)
                .extracting(PlaySuspectResponse::name)
                .containsExactly("후보A", "증인");
        String json = objectMapper.writeValueAsString(suspects);
        assertThat(json)
                .doesNotContain("culpritEligible")
                .doesNotContain("suspicionLevel");
    }
}
