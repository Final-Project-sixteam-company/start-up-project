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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class CustomScenarioServiceSolutionTest {

    @Autowired private CustomScenarioService customScenarioService;
    @Autowired private ScenarioRepository scenarioRepository;
    @Autowired private SuspectRepository suspectRepository;
    @Autowired private EvidenceRepository evidenceRepository;
    @Autowired private SolutionRepository solutionRepository;

    private static final Long OWNER_USER_ID = 100L;
    private static final Long OTHER_USER_ID = 999L;
    private Scenario savedScenario;
    private Suspect culpritSuspect;
    private Evidence keyEvidence;

    @BeforeEach
    void setUp() {
        savedScenario = scenarioRepository.save(Scenario.builder()
                .title("커스텀 시나리오 정답 테스트")
                .description("테스트용")
                .scenarioType(ScenarioType.CUSTOM)
                .visibility(ScenarioVisibility.PRIVATE)
                .difficulty(Difficulty.NORMAL)
                .estimatedPlayTimeMinutes(20)
                .creatorId(OWNER_USER_ID)
                .status(ScenarioStatus.DRAFT)
                .build());

        culpritSuspect = suspectRepository.save(Suspect.builder()
                .scenarioId(savedScenario.getId())
                .code("SUS_01")
                .name("범인용의자")
                .role("역할")
                .characterType("NPC")
                .culpritEligible(true)
                .sortOrder(1)
                .build());

        keyEvidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(savedScenario.getId())
                .code("EVD_01")
                .title("핵심증거")
                .description("설명")
                .evidenceType(EvidenceType.PHYSICAL)
                .importance(EvidenceImportance.HIGH)
                .sortOrder(1)
                .build());
    }

    @AfterEach
    void tearDown() {
        solutionRepository.deleteAllInBatch();
        evidenceRepository.deleteAllInBatch();
        suspectRepository.deleteAllInBatch();
        scenarioRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("정답 등록 및 수정(UPSERT) 성공")
    void createOrUpdateSolution_success() {
        // given
        CustomSolutionCreateRequest request = new CustomSolutionCreateRequest();
        ReflectionTestUtils.setField(request, "culpritSuspectId", culpritSuspect.getId());
        ReflectionTestUtils.setField(request, "motive", "원한");
        ReflectionTestUtils.setField(request, "method", "독살");
        ReflectionTestUtils.setField(request, "coverUp", "알리바이 조작");
        ReflectionTestUtils.setField(request, "fullExplanation", "전체 설명");
        ReflectionTestUtils.setField(request, "keyEvidenceIds", List.of(keyEvidence.getId()));

        // when
        CustomSolutionCreateResponse response = customScenarioService.createOrUpdateSolution(OWNER_USER_ID, savedScenario.getId(), request);

        // then
        assertThat(response.getSolutionId()).isNotNull();
        Solution solution = solutionRepository.findByScenarioId(savedScenario.getId()).orElseThrow();
        assertThat(solution.getMotive()).isEqualTo("원한");
        assertThat(solution.getKeyEvidenceIds()).isEqualTo(keyEvidence.getId().toString());
    }

    @Test
    @DisplayName("정답 등록 실패 - 해당 시나리오 소속이 아닌 용의자 지정 시 예외 발생")
    void createOrUpdateSolution_fail_invalid_suspect() {
        // given
        CustomSolutionCreateRequest request = new CustomSolutionCreateRequest();
        ReflectionTestUtils.setField(request, "culpritSuspectId", 9999L);
        ReflectionTestUtils.setField(request, "keyEvidenceIds", List.of(keyEvidence.getId()));

        // when & then
        assertThatThrownBy(() -> customScenarioService.createOrUpdateSolution(OWNER_USER_ID, savedScenario.getId(), request))
                .isInstanceOf(ScenarioException.class)
                .hasMessageContaining(ScenarioErrorCode.INVALID_SUSPECT_OWNERSHIP.getMessage());
    }

    @Test
    @DisplayName("정답 조회(GET) 성공")
    void getSolution_success() {
        // given
        solutionRepository.save(Solution.builder()
                .scenarioId(savedScenario.getId())
                .culpritSuspectId(culpritSuspect.getId())
                .motive("테스트 동기")
                .method("테스트 방법")
                .coverUp("은폐")
                .fullExplanation("전체 설명")
                .keyEvidenceIds(keyEvidence.getId().toString())
                .build());

        // when
        CustomSolutionResponse response = customScenarioService.getSolution(OWNER_USER_ID, savedScenario.getId());

        // then
        assertThat(response.getMotive()).isEqualTo("테스트 동기");
        assertThat(response.getCulpritSuspectId()).isEqualTo(culpritSuspect.getId());
        assertThat(response.getKeyEvidenceIds()).containsExactly(keyEvidence.getId());
    }

    @Test
    @DisplayName("정답 조회(GET) 실패 - 정답이 없는 경우 예외 발생")
    void getSolution_fail_not_found() {
        // when & then
        assertThatThrownBy(() -> customScenarioService.getSolution(OWNER_USER_ID, savedScenario.getId()))
                .isInstanceOf(ScenarioException.class)
                .hasMessageContaining(ScenarioErrorCode.SOLUTION_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("타인의 시나리오 정답 조회 시나리오 접근 권한 예외 발생")
    void getSolution_fail_unauthorized() {
        // given
        solutionRepository.save(Solution.builder()
                .scenarioId(savedScenario.getId())
                .culpritSuspectId(culpritSuspect.getId())
                .motive("동기")
                .method("방법")
                .coverUp("은폐")
                .fullExplanation("전체 설명")
                .keyEvidenceIds(keyEvidence.getId().toString())
                .build());

        // when & then
        assertThatThrownBy(() -> customScenarioService.getSolution(OTHER_USER_ID, savedScenario.getId()))
                .isInstanceOf(ScenarioException.class)
                .hasMessageContaining(ScenarioErrorCode.SCENARIO_ACCESS_DENIED.getMessage());
    }
}
