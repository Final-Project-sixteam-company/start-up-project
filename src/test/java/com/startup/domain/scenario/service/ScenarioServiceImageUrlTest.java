package com.startup.domain.scenario.service;

import com.startup.common.dto.PageResponse;
import com.startup.domain.scenario.dto.ScenarioDetailResponse;
import com.startup.domain.scenario.dto.ScenarioSearchCondition;
import com.startup.domain.scenario.dto.ScenarioSummaryResponse;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.Difficulty;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioType;
import com.startup.domain.scenario.enums.ScenarioVisibility;
import com.startup.domain.scenario.repository.ScenarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "clueroom.assets.public-base-url=https://assets.example.com")
class ScenarioServiceImageUrlTest {

    @Autowired
    private ScenarioService scenarioService;

    @Autowired
    private ScenarioRepository scenarioRepository;

    @AfterEach
    void tearDown() {
        scenarioRepository.deleteAllInBatch();
    }

    @Test
    void getScenarios_resolvesThumbnailUrlFromCoverAssetKey() {
        scenarioRepository.save(Scenario.builder()
                .title("image scenario")
                .description("scenario thumbnail test")
                .scenarioType(ScenarioType.OFFICIAL)
                .visibility(ScenarioVisibility.PUBLIC)
                .difficulty(Difficulty.NORMAL)
                .coverAssetKey("official/seowolchae/v1/scenario/SCENARIO.cover.png")
                .status(ScenarioStatus.PUBLISHED)
                .build());

        PageResponse<ScenarioSummaryResponse> response = scenarioService.getScenarios(
                1L, new ScenarioSearchCondition(), PageRequest.of(0, 10));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().thumbnailUrl())
                .isEqualTo("https://assets.example.com/official/seowolchae/v1/scenario/SCENARIO.cover.png");
        assertThat(response.content().getFirst().canPlay()).isTrue();
    }

    @Test
    void getScenario_resolvesCoverAndMapImageUrls() {
        Scenario scenario = scenarioRepository.save(Scenario.builder()
                .title("detail image scenario")
                .description("scenario detail image test")
                .scenarioType(ScenarioType.OFFICIAL)
                .visibility(ScenarioVisibility.PUBLIC)
                .difficulty(Difficulty.NORMAL)
                .coverAssetKey("official/studio9/v1/scenario/SCENARIO_STUDIO9.cover.png")
                .mapAssetKey("official/studio9/v1/scenario/SCENARIO_STUDIO9.map.png")
                .status(ScenarioStatus.PUBLISHED)
                .build());

        ScenarioDetailResponse response = scenarioService.getScenario(1L, scenario.getId());

        assertThat(response.coverImageUrl())
                .isEqualTo("https://assets.example.com/official/studio9/v1/scenario/SCENARIO_STUDIO9.cover.png");
        assertThat(response.mapImageUrl())
                .isEqualTo("https://assets.example.com/official/studio9/v1/scenario/SCENARIO_STUDIO9.map.png");
    }
}
