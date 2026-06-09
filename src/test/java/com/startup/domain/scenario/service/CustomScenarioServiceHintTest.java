package com.startup.domain.scenario.service;

import com.startup.domain.scenario.dto.*;
import com.startup.domain.scenario.entity.*;
import com.startup.domain.scenario.enums.*;
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

@SpringBootTest
public class CustomScenarioServiceHintTest {

    @Autowired private CustomScenarioService customScenarioService;
    @Autowired private ScenarioRepository scenarioRepository;
    @Autowired private HintRepository hintRepository;

    private static final Long OWNER_USER_ID = 100L;
    private static final Long OTHER_USER_ID = 999L;
    private Scenario savedScenario;

    @BeforeEach
    void setUp() {
        savedScenario = scenarioRepository.save(Scenario.builder()
                .title("커스텀 시나리오 힌트 테스트")
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
        hintRepository.deleteAllInBatch();
        scenarioRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("힌트 생성 성공")
    void createHint_success() {
        // given
        CustomHintCreateRequest request = new CustomHintCreateRequest();
        ReflectionTestUtils.setField(request, "content", "힌트 내용입니다.");
        ReflectionTestUtils.setField(request, "penaltyScore", 10);
        ReflectionTestUtils.setField(request, "hintLevel", 1);

        // when
        CustomHintCreateResponse response = customScenarioService.createHint(OWNER_USER_ID, savedScenario.getId(), request);

        // then
        assertThat(response.getHintId()).isNotNull();
        Hint hint = hintRepository.findById(response.getHintId()).orElseThrow();
        assertThat(hint.getContent()).isEqualTo("힌트 내용입니다.");
        assertThat(hint.getHintLevel()).isEqualTo(1);
    }
}
