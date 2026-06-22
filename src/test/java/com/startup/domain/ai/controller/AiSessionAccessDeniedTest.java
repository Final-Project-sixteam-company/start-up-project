package com.startup.domain.ai.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.error.BusinessException;
import com.startup.common.error.CommonErrorCode;
import com.startup.domain.ai.dto.FinalDeductionRequest;
import com.startup.domain.ai.dto.InterrogationRequest;
import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.repository.PlaySessionRepository;
import com.startup.domain.ai.service.AiInterrogationService;
import com.startup.domain.ai.service.AiDeductionScorer;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.Difficulty;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioType;
import com.startup.domain.scenario.enums.ScenarioVisibility;
import com.startup.domain.scenario.repository.ScenarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AiSessionAccessDeniedTest {

    @Autowired
    private AiInterrogationService aiInterrogationService;

    @Autowired
    private AiDeductionScorer aiDeductionScorer;

    @Autowired
    private MockUserProvider mockUserProvider;

    @Autowired
    private PlaySessionRepository playSessionRepository;

    @Autowired
    private ScenarioRepository scenarioRepository;

    private Long sessionId;
    private final Long OWNER_USER_ID = 100L;
    private final Long OTHER_USER_ID = 999L;

    @BeforeEach
    void setUp() {
        Scenario scenario = scenarioRepository.save(
                Scenario.builder()
                        .title("접근 권한 테스트 시나리오")
                        .description("설명")
                        .scenarioType(ScenarioType.OFFICIAL)
                        .visibility(ScenarioVisibility.PUBLIC)
                        .difficulty(Difficulty.NORMAL)
                        .status(ScenarioStatus.PUBLISHED)
                        .build()
        );

        PlaySession session = playSessionRepository.save(
                PlaySession.builder()
                        .userId(OWNER_USER_ID)
                        .scenarioId(scenario.getId())
                        .build()
        );
        sessionId = session.getId();
    }

    @AfterEach
    void tearDown() {
        playSessionRepository.deleteAllInBatch();
        scenarioRepository.deleteAllInBatch();
        
        // 싱글톤 상태 복구
        ReflectionTestUtils.setField(mockUserProvider, "mockUserId", 1L);
    }

    @Test
    @DisplayName("타인의 세션에 심문을 시도하면 ACCESS_DENIED 예외가 발생한다")
    void interrogate_AccessDenied() {
        // given (현재 접속자를 타인으로 조작)
        ReflectionTestUtils.setField(mockUserProvider, "mockUserId", OTHER_USER_ID);

        InterrogationRequest request = new InterrogationRequest(1L, com.startup.domain.ai.enums.QuestionType.FREE, "테스트 질문", null);

        // when & then
        assertThatThrownBy(() -> aiInterrogationService.interrogate(sessionId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(CommonErrorCode.ACCESS_DENIED.getMessage());
    }

    @Test
    @DisplayName("타인의 세션에 최종 추리를 제출하면 ACCESS_DENIED 예외가 발생한다")
    void finalDeduction_AccessDenied() {
        // given (현재 접속자를 타인으로 조작)
        ReflectionTestUtils.setField(mockUserProvider, "mockUserId", OTHER_USER_ID);

        FinalDeductionRequest request = new FinalDeductionRequest(1L, "동기", "트릭", "은폐", java.util.List.of(1L));

        // when & then
        assertThatThrownBy(() -> aiDeductionScorer.submitAndScore(sessionId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(CommonErrorCode.ACCESS_DENIED.getMessage());
    }
}
