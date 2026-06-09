package com.startup.domain.scenario.service;

import com.startup.domain.scenario.dto.*;
import com.startup.domain.scenario.entity.*;
import com.startup.domain.scenario.enums.*;
import com.startup.domain.scenario.error.ScenarioErrorCode;
import com.startup.domain.scenario.error.ScenarioException;
import com.startup.domain.scenario.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class CustomScenarioServiceLocationTest {

    @Autowired private CustomScenarioService customScenarioService;
    @Autowired private ScenarioRepository scenarioRepository;
    @Autowired private ScenarioLocationRepository locationRepository;

    private static final Long OWNER_USER_ID = 100L;
    private static final Long OTHER_USER_ID = 999L;
    private Scenario savedScenario;

    @BeforeEach
    void setUp() {
        savedScenario = scenarioRepository.save(Scenario.builder()
                .title("커스텀 시나리오 장소 테스트")
                .description("테스트용")
                .scenarioType(ScenarioType.CUSTOM)
                .visibility(ScenarioVisibility.PRIVATE)
                .difficulty(Difficulty.NORMAL)
                .estimatedPlayTimeMinutes(20)
                .creatorId(OWNER_USER_ID)
                .status(ScenarioStatus.DRAFT)
                .build());
    }

    @AfterEach
    void tearDown() {
        locationRepository.deleteAllInBatch();
        scenarioRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("장소 등록 성공")
    void createLocation_success() {
        // given
        CustomLocationCreateRequest request = new CustomLocationCreateRequest();
        ReflectionTestUtils.setField(request, "name", "거실");
        ReflectionTestUtils.setField(request, "description", "피해자가 발견된 곳");

        // when
        CustomLocationCreateResponse response = customScenarioService.createLocation(OWNER_USER_ID, savedScenario.getId(), request);

        // then
        assertThat(response.getLocationId()).isNotNull();
        ScenarioLocation location = locationRepository.findById(response.getLocationId()).orElseThrow();
        assertThat(location.getName()).isEqualTo("거실");
    }

}
