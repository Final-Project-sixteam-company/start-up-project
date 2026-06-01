package com.startup.domain.play.service;

import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.repository.PlaySessionRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PlaySessionEndTest {

    @Autowired
    private PlaySessionService playSessionService;

    @Autowired
    private PlaySessionRepository playSessionRepository;

    @Autowired
    private ScenarioRepository scenarioRepository;

    private Long sessionId;
    private final Long USER_ID = 100L;

    @BeforeEach
    void setUp() {
        Scenario scenario = scenarioRepository.save(
                Scenario.builder()
                        .title("종료 테스트 시나리오")
                        .description("설명")
                        .scenarioType(ScenarioType.OFFICIAL)
                        .visibility(ScenarioVisibility.PUBLIC)
                        .difficulty(Difficulty.NORMAL)
                        .status(ScenarioStatus.PUBLISHED)
                        .build()
        );

        PlaySession session = playSessionRepository.save(
                PlaySession.builder()
                        .userId(USER_ID)
                        .scenarioId(scenario.getId())
                        .build()
        );
        // PlaySession 생성 시 activeKey가 자동 할당됨
        sessionId = session.getId();
    }

    @AfterEach
    void tearDown() {
        playSessionRepository.deleteAllInBatch();
        scenarioRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("세션 포기(Abandon) 시 activeKey가 null로 초기화된다")
    void abandonSession_ClearsActiveKey() {
        // given
        PlaySession beforeSession = playSessionRepository.findById(sessionId).orElseThrow();
        assertThat(beforeSession.getActiveKey()).isNotNull();

        // when
        playSessionService.abandonSession(USER_ID, sessionId);

        // then
        PlaySession afterSession = playSessionRepository.findById(sessionId).orElseThrow();
        assertThat(afterSession.getActiveKey()).isNull();
        assertThat(afterSession.getStatus()).isEqualTo(com.startup.domain.play.enums.PlaySessionStatus.ABANDONED);
    }

    @Test
    @DisplayName("세션 완료(Complete) 시 activeKey가 null로 초기화된다")
    void completeSession_ClearsActiveKey() {
        // given
        PlaySession beforeSession = playSessionRepository.findById(sessionId).orElseThrow();
        assertThat(beforeSession.getActiveKey()).isNotNull();

        // when
        playSessionService.completeSession(sessionId);

        // then
        PlaySession afterSession = playSessionRepository.findById(sessionId).orElseThrow();
        assertThat(afterSession.getActiveKey()).isNull();
        assertThat(afterSession.getStatus()).isEqualTo(com.startup.domain.play.enums.PlaySessionStatus.COMPLETED);
    }
}
