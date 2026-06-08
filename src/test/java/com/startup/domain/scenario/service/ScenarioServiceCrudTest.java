package com.startup.domain.scenario.service;

import com.startup.domain.scenario.dto.*;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.entity.Solution;
import com.startup.domain.scenario.entity.Suspect;
import com.startup.domain.scenario.entity.Evidence;
import com.startup.domain.scenario.entity.Hint;
import com.startup.domain.scenario.enums.*;
import com.startup.domain.scenario.error.ScenarioException;
import com.startup.domain.scenario.error.ScenarioErrorCode;
import com.startup.domain.scenario.repository.*;
import com.startup.domain.ai.entity.ScenarioValidationResult;
import com.startup.domain.ai.repository.ScenarioValidationResultRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class ScenarioServiceCrudTest {

    @Autowired private ScenarioService scenarioService;
    @Autowired private ScenarioRepository scenarioRepository;
    @Autowired private SuspectRepository suspectRepository;
    @Autowired private EvidenceRepository evidenceRepository;
    @Autowired private SolutionRepository solutionRepository;
    @Autowired private HintRepository hintRepository;
    @Autowired private ScenarioValidationResultRepository validationResultRepository;

    // ──────────────────────────────────────────
    // 공통 기본 세팅
    // ──────────────────────────────────────────
    private static final Long OWNER_USER_ID = 100L;    // 시나리오 작성자
    private static final Long OTHER_USER_ID = 999L;    // 다른 유저 (타인)
    private Scenario savedScenario;   // 테스트용 기본 시나리오 (DRAFT 상태)

    @BeforeEach
    void setUp() {
        // 모든 테스트가 시작되기 전에, DRAFT 상태의 기본 시나리오 하나를 미리 만들어 둡니다.
        savedScenario = scenarioRepository.save(Scenario.builder()
                .title("테스트 사건")
                .description("테스트용 시나리오 설명")
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
        validationResultRepository.deleteAllInBatch();
        hintRepository.deleteAllInBatch();
        solutionRepository.deleteAllInBatch();
        evidenceRepository.deleteAllInBatch();
        suspectRepository.deleteAllInBatch();
        scenarioRepository.deleteAllInBatch();
    }

    // ================================================
    // 1. 시나리오 생성 테스트 (POST /api/scenarios)
    // ================================================
    @Test
    @DisplayName("생성 성공: status=DRAFT, visibility=PRIVATE, type=CUSTOM으로 강제 저장된다")
    void createScenario_success_forcedDefaults() {
        // given
        ScenarioCreateRequest request = new ScenarioCreateRequest(
                "새 사건", "새 설명", null, Difficulty.HARD, 1, 3, 30
        );

        // when
        ScenarioCreateResponse response = scenarioService.createScenario(OWNER_USER_ID, request);

        // then
        assertThat(response.scenarioId()).isNotNull();
        assertThat(response.status()).isEqualTo(ScenarioStatus.DRAFT);

        // DB 확인
        Scenario created = scenarioRepository.findById(response.scenarioId()).orElseThrow();
        assertThat(created.getScenarioType()).isEqualTo(ScenarioType.CUSTOM);
        assertThat(created.getVisibility()).isEqualTo(ScenarioVisibility.PRIVATE);
        assertThat(created.getCreatorId()).isEqualTo(OWNER_USER_ID);
    }

    // ================================================
    // 2. 시나리오 수정 테스트 (PATCH /api/scenarios/{id})
    // ================================================
    @Test
    @DisplayName("수정 성공: title만 보내면 title만 바뀌고, description은 기존 값이 유지된다")
    void updateScenario_success_partialUpdate() {
        // given
        ScenarioUpdateRequest request = new ScenarioUpdateRequest(
                "변경된 제목", null, null, null
        );

        // when
        ScenarioUpdateResponse response = scenarioService.updateScenario(
                OWNER_USER_ID, savedScenario.getId(), request
        );

        // then
        assertThat(response.updated()).isTrue();
        Scenario updated = scenarioRepository.findById(savedScenario.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("변경된 제목");
        assertThat(updated.getDescription()).isEqualTo("테스트용 시나리오 설명"); // 기존 값 유지!
    }

    @Test
    @DisplayName("수정 실패: 타인의 시나리오를 수정하려고 하면 ACCESS_DENIED 예외 발생")
    void updateScenario_fail_otherUserAccessDenied() {
        ScenarioUpdateRequest request = new ScenarioUpdateRequest(
                "해킹 시도", null, null, null
        );

        // OTHER_USER_ID(999)가 OWNER_USER_ID(100)의 시나리오를 수정
        assertThatThrownBy(() ->
                scenarioService.updateScenario(OTHER_USER_ID, savedScenario.getId(), request)
        ).isInstanceOf(ScenarioException.class);
    }

    @Test
    @DisplayName("수정 실패: PUBLISHED 상태의 시나리오는 수정할 수 없다")
    void updateScenario_fail_publishedCannotModify() {
        // given: 시나리오를 PUBLISHED 상태로 바꿔놓습니다
        savedScenario.publish(ScenarioVisibility.PUBLIC);
        scenarioRepository.saveAndFlush(savedScenario);

        ScenarioUpdateRequest request = new ScenarioUpdateRequest(
                "수정 시도", null, null, null
        );

        // when & then: PUBLISHED 상태에서 수정하면 예외가 터져야 합니다
        assertThatThrownBy(() ->
                scenarioService.updateScenario(OWNER_USER_ID, savedScenario.getId(), request)
        ).isInstanceOf(ScenarioException.class);
    }

    @Test
    @DisplayName("수정 실패: creatorId가 null인 공식 시나리오 수정 시 NPE 대신 ACCESS_DENIED 예외 발생")
    void updateScenario_fail_officialScenarioNullCreatorId() {
        // given: 공식 시나리오 (creatorId = null) 생성
        Scenario officialScenario = scenarioRepository.save(Scenario.builder()
                .title("공식 사건")
                .description("설명")
                .scenarioType(ScenarioType.OFFICIAL)
                .visibility(ScenarioVisibility.PUBLIC)
                .difficulty(Difficulty.NORMAL)
                .status(ScenarioStatus.PUBLISHED)
                .creatorId(null)
                .build());

        ScenarioUpdateRequest request = new ScenarioUpdateRequest(
                "수정 시도", null, null, null
        );

        // when & then: NPE가 터지지 않고 정해진 예외가 발생해야 함
        assertThatThrownBy(() ->
                scenarioService.updateScenario(OWNER_USER_ID, officialScenario.getId(), request)
        ).isInstanceOf(ScenarioException.class)
         .hasMessageContaining(ScenarioErrorCode.SCENARIO_ACCESS_DENIED.getMessage());
    }

    // ================================================
    // 3. 시나리오 발행 테스트 (POST /api/scenarios/{id}/publish)
    // ================================================
    @Test
    @DisplayName("발행 성공: 모든 필수 데이터가 있으면 PUBLISHED로 전환된다")
    void publishScenario_success() {
        // given
        Long scenarioId = savedScenario.getId();

        // 용의자 2명 (최소 2명 필요)
        Suspect suspect = suspectRepository.save(Suspect.builder()
                .scenarioId(scenarioId)
                .name("용의자A")
                .role("비서")
                .sortOrder(1)
                .build());
        suspectRepository.save(Suspect.builder()
                .scenarioId(scenarioId)
                .name("용의자B")
                .role("경호원")
                .sortOrder(2)
                .build());

        // 증거 3개 (최소 3개 필요)
        Evidence evidence1 = evidenceRepository.save(Evidence.builder()
                .scenarioId(scenarioId)
                .title("증거1")
                .description("증거 설명1")
                .sortOrder(1)
                .build());
        Evidence evidence2 = evidenceRepository.save(Evidence.builder()
                .scenarioId(scenarioId)
                .title("증거2")
                .description("증거 설명2")
                .sortOrder(2)
                .build());
        evidenceRepository.save(Evidence.builder()
                .scenarioId(scenarioId)
                .title("증거3")
                .description("증거 설명3")
                .sortOrder(3)
                .build());

        // 정답: 채점 필수 필드(motive, method, coverUp) + 실제 증거 ID를 핵심 증거로 지정
        solutionRepository.save(Solution.builder()
                .scenarioId(scenarioId)
                .culpritSuspectId(suspect.getId())
                .motive("돈")
                .method("독살")
                .coverUp("시체 유기")
                .keyEvidenceIds(evidence1.getId() + "," + evidence2.getId())
                .build());

        // 힌트 1개 (최소 1개 필요)
        hintRepository.save(Hint.builder()
                .scenarioId(scenarioId)
                .hintLevel(1)
                .content("힌트 내용")
                .penaltyScore(10)
                .build());

        // AI 검증 통과(PASSED) 기록 세팅 (flush를 호출해 DB에 즉시 반영)
        validationResultRepository.saveAndFlush(ScenarioValidationResult.builder()
                .scenarioId(scenarioId)
                .validationStatus("PASSED")
                .validationScore(100)
                .problemSummary("문제 없음")
                .suggestion("훌륭한 시나리오입니다.")
                .checkItemsJson("{}")
                .build());

        // when
        ScenarioPublishResponse response = scenarioService.publishScenario(
                OWNER_USER_ID, scenarioId, new ScenarioPublishRequest(ScenarioVisibility.PUBLIC)
        );

        // then
        assertThat(response.status()).isEqualTo(ScenarioStatus.PUBLISHED);
        assertThat(response.visibility()).isEqualTo(ScenarioVisibility.PUBLIC);
        assertThat(response.publishedAt()).isNotNull();
    }

    @Test
    @DisplayName("발행 실패: 용의자가 없으면 정합성 검증 예외 발생")
    void publishScenario_fail_noSuspect() {
        // given: 용의자 없이 증거와 정답만 있는 상태
        Long scenarioId = savedScenario.getId();
        evidenceRepository.save(Evidence.builder()
                .scenarioId(scenarioId)
                .title("증거1")
                .description("증거 설명")
                .sortOrder(1)
                .build());

        // 용의자가 없으므로 발행 시 예외가 발생해야 한다
        assertThatThrownBy(() ->
                scenarioService.publishScenario(
                        OWNER_USER_ID, scenarioId, new ScenarioPublishRequest(ScenarioVisibility.PUBLIC)
                )
        ).isInstanceOf(ScenarioException.class);
    }

    @Test
    @DisplayName("발행 실패: 정답(Solution)이 없으면 정합성 검증 예외 발생")
    void publishScenario_fail_noSolution() {
        // given: 용의자와 증거는 있지만 정답이 없는 상태
        Long scenarioId = savedScenario.getId();

        suspectRepository.save(Suspect.builder()
                .scenarioId(scenarioId)
                .name("용의자A")
                .role("비서")
                .sortOrder(1)
                .build());

        evidenceRepository.save(Evidence.builder()
                .scenarioId(scenarioId)
                .title("증거1")
                .description("증거 설명")
                .sortOrder(1)
                .build());

        // 정답(Solution)이 없으므로 발행 시 예외가 발생해야 한다
        assertThatThrownBy(() ->
                scenarioService.publishScenario(
                        OWNER_USER_ID, scenarioId, new ScenarioPublishRequest(ScenarioVisibility.PUBLIC)
                )
        ).isInstanceOf(ScenarioException.class);
    }

    @Test
    @DisplayName("발행 실패: 진범이 해당 시나리오의 용의자가 아니면 정합성 검증 예외 발생")
    void publishScenario_fail_invalidCulprit() {
        // given: 정상 용의자, 증거 세팅
        Long scenarioId = savedScenario.getId();

        suspectRepository.save(Suspect.builder()
                .scenarioId(scenarioId)
                .name("용의자A")
                .role("비서")
                .sortOrder(1)
                .build());

        evidenceRepository.save(Evidence.builder()
                .scenarioId(scenarioId)
                .title("증거1")
                .description("증거 설명")
                .sortOrder(1)
                .build());

        // 다른 시나리오의 용의자라고 가정하고 존재하지 않는 9999L을 진범으로 설정
        solutionRepository.save(Solution.builder()
                .scenarioId(scenarioId)
                .culpritSuspectId(9999L)
                .motive("돈")
                .method("독살")
                .build());

        // 정답의 진범이 현재 시나리오의 용의자가 아니므로 발행 시 예외가 발생해야 한다
        assertThatThrownBy(() ->
                scenarioService.publishScenario(
                        OWNER_USER_ID, scenarioId, new ScenarioPublishRequest(ScenarioVisibility.PUBLIC)
                )
        ).isInstanceOf(ScenarioException.class)
         .hasMessageContaining("정답의 범인이 현재 시나리오의 용의자가 아님");
    }
}
