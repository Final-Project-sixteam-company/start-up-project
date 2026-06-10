package com.startup.domain.scenario.service;

import com.startup.common.error.BusinessException;
import com.startup.domain.scenario.dto.*;
import com.startup.domain.scenario.entity.*;
import com.startup.domain.scenario.enums.*;
import com.startup.domain.scenario.error.ScenarioErrorCode;
import com.startup.domain.scenario.error.ScenarioException;
import com.startup.domain.scenario.repository.*;
import com.startup.domain.ai.repository.SuspectResponsePolicyRepository;
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
public class CustomScenarioServiceEvidenceTest {

    @Autowired private CustomScenarioService customScenarioService;
    @Autowired private ScenarioRepository scenarioRepository;
    @Autowired private SuspectRepository suspectRepository;
    @Autowired private EvidenceRepository evidenceRepository;
    @Autowired private SolutionRepository solutionRepository;
    @Autowired private EvidenceSuspectRepository evidenceSuspectRepository;
    @Autowired private EvidenceUnlockRuleRepository evidenceUnlockRuleRepository;
    @Autowired private SuspectResponsePolicyRepository suspectResponsePolicyRepository;

    private static final Long OWNER_USER_ID = 100L;
    private static final Long OTHER_USER_ID = 999L;
    private Scenario savedScenario;
    private Suspect savedSuspect;

    @BeforeEach
    void setUp() {
        savedScenario = scenarioRepository.save(Scenario.builder()
                .title("커스텀 시나리오 증거 테스트")
                .description("테스트용")
                .scenarioType(ScenarioType.CUSTOM)
                .visibility(ScenarioVisibility.PRIVATE)
                .difficulty(Difficulty.NORMAL)
                .estimatedPlayTimeMinutes(20)
                .creatorId(OWNER_USER_ID)
                .status(ScenarioStatus.DRAFT)
                .build());

        savedSuspect = suspectRepository.save(Suspect.builder()
                .scenarioId(savedScenario.getId())
                .code("SUS_01")
                .name("용의자A")
                .role("역할")
                .characterType("NPC")
                .culpritEligible(true)
                .sortOrder(1)
                .build());
    }

    @AfterEach
    void tearDown() {
        solutionRepository.deleteAllInBatch();
        evidenceUnlockRuleRepository.deleteAllInBatch();
        suspectResponsePolicyRepository.deleteAllInBatch();
        evidenceSuspectRepository.deleteAllInBatch();
        evidenceRepository.deleteAllInBatch();
        suspectRepository.deleteAllInBatch();
        scenarioRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("증거 등록 성공 및 관련 용의자 매핑 확인")
    void createEvidence_success() {
        // given
        CustomEvidenceCreateRequest request = new CustomEvidenceCreateRequest();
        ReflectionTestUtils.setField(request, "title", "피묻은 칼");
        ReflectionTestUtils.setField(request, "description", "설명");
        ReflectionTestUtils.setField(request, "evidenceType", EvidenceType.PHYSICAL);
        ReflectionTestUtils.setField(request, "relatedSuspectIds", List.of(savedSuspect.getId()));

        // when
        CustomEvidenceCreateResponse response = customScenarioService.createEvidence(OWNER_USER_ID, savedScenario.getId(), request);

        // then
        assertThat(response.getEvidenceId()).isNotNull();
        Evidence evidence = evidenceRepository.findById(response.getEvidenceId()).orElseThrow();
        assertThat(evidence.getTitle()).isEqualTo("피묻은 칼");
        assertThat(evidenceSuspectRepository.findAllByEvidenceIdIn(List.of(evidence.getId()))).hasSize(1);
    }

    @Test
    @DisplayName("증거 수정 성공")
    void updateEvidence_success() {
        // given
        Evidence evidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(savedScenario.getId())
                .code("EVD_01")
                .title("기존증거")
                .description("설명")
                .evidenceType(EvidenceType.PHYSICAL)
                .importance(EvidenceImportance.NORMAL)
                .sortOrder(1)
                .build());

        CustomEvidenceUpdateRequest request = new CustomEvidenceUpdateRequest();
        ReflectionTestUtils.setField(request, "title", "변경증거");
        ReflectionTestUtils.setField(request, "evidenceType", EvidenceType.DOCUMENT);
        ReflectionTestUtils.setField(request, "importance", EvidenceImportance.HIGH);

        // when
        CustomEvidenceResponse response = customScenarioService.updateEvidence(OWNER_USER_ID, evidence.getId(), request);

        // then
        assertThat(response.getTitle()).isEqualTo("변경증거");
        assertThat(response.getEvidenceType()).isEqualTo(EvidenceType.DOCUMENT);
        assertThat(response.getImportance()).isEqualTo(EvidenceImportance.HIGH);
    }

    @Test
    @DisplayName("증거 수정 성공 - 해금 타입을 NONE으로 변경 시 기존 조건 초기화")
    void updateEvidence_success_clear_conditions_when_none() {
        // given
        Evidence evidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(savedScenario.getId())
                .code("EVD_01")
                .title("기존증거")
                .description("설명")
                .evidenceType(EvidenceType.PHYSICAL)
                .importance(EvidenceImportance.NORMAL)
                .unlockType(EvidenceUnlockType.TIME)
                .unlockAfterMinutes(10)
                .unlockPhase("1")
                .sortOrder(1)
                .build());

        CustomEvidenceUpdateRequest request = new CustomEvidenceUpdateRequest();
        ReflectionTestUtils.setField(request, "unlockType", EvidenceUnlockType.NONE);

        // when
        CustomEvidenceResponse response = customScenarioService.updateEvidence(OWNER_USER_ID, evidence.getId(), request);

        // then
        assertThat(response.getUnlockType()).isEqualTo(EvidenceUnlockType.NONE);
        assertThat(response.getUnlockAfterMinutes()).isNull();
        assertThat(response.getUnlockConditionJson()).isNull();
        assertThat(response.getUnlockPhase()).isNull();

        Evidence updatedEvidence = evidenceRepository.findById(evidence.getId()).orElseThrow();
        assertThat(updatedEvidence.getUnlockAfterMinutes()).isNull();
        assertThat(updatedEvidence.getUnlockConditionJson()).isNull();
        assertThat(updatedEvidence.getUnlockPhase()).isNull();
    }

    @Test
    @DisplayName("증거 수정 실패 - 자기 자신을 해금 조건으로 설정 시 예외 발생")
    void updateEvidence_fail_when_self_triggering() {
        // given
        Evidence evidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(savedScenario.getId())
                .code("EVD_01")
                .title("기존증거")
                .description("설명")
                .evidenceType(EvidenceType.PHYSICAL)
                .importance(EvidenceImportance.NORMAL)
                .sortOrder(1)
                .build());

        CustomEvidenceUpdateRequest request = new CustomEvidenceUpdateRequest();
        ReflectionTestUtils.setField(request, "unlockType", EvidenceUnlockType.EVIDENCE_PRESENTED);
        ReflectionTestUtils.setField(request, "unlockConditionJson", "{\"requiredPresentedEvidenceId\": " + evidence.getId() + "}");

        // when & then
        assertThatThrownBy(() -> customScenarioService.updateEvidence(OWNER_USER_ID, evidence.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("자기 자신을 해금 조건으로 설정할 수 없습니다.");
    }

    @Test
    @DisplayName("증거 수정 실패 - 자기 자신을 선행 해금 증거(requiredEvidenceIds)로 설정 시 예외 발생")
    void updateEvidence_fail_when_self_prerequisite() {
        // given
        Evidence existingEvidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(savedScenario.getId())
                .code("EVD_01")
                .title("제시할증거")
                .description("설명")
                .evidenceType(EvidenceType.PHYSICAL)
                .importance(EvidenceImportance.NORMAL)
                .sortOrder(1)
                .build());

        Evidence evidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(savedScenario.getId())
                .code("EVD_02")
                .title("타겟증거")
                .description("설명")
                .evidenceType(EvidenceType.PHYSICAL)
                .importance(EvidenceImportance.NORMAL)
                .sortOrder(2)
                .build());

        CustomEvidenceUpdateRequest request = new CustomEvidenceUpdateRequest();
        ReflectionTestUtils.setField(request, "unlockType", EvidenceUnlockType.EVIDENCE_PRESENTED);
        ReflectionTestUtils.setField(request, "unlockConditionJson", 
                "{\"requiredPresentedEvidenceId\": " + existingEvidence.getId() + ", \"requiredEvidenceIds\": [" + evidence.getId() + "]}");

        // when & then
        assertThatThrownBy(() -> customScenarioService.updateEvidence(OWNER_USER_ID, evidence.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("자기 자신을 선행 해금 증거로 설정할 수 없습니다.");
    }

    @Test
    @DisplayName("증거 삭제 성공 (고아 데이터 동시 삭제)")
    void deleteEvidence_success() {
        // given
        Evidence evidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(savedScenario.getId())
                .code("EVD_01")
                .title("삭제할증거")
                .description("설명")
                .evidenceType(EvidenceType.PHYSICAL)
                .importance(EvidenceImportance.NORMAL)
                .sortOrder(1)
                .build());

        // when
        customScenarioService.deleteEvidence(OWNER_USER_ID, evidence.getId());

        // then
        assertThat(evidenceRepository.findById(evidence.getId())).isEmpty();
    }

    @Test
    @DisplayName("증거 삭제 실패 - 정답의 핵심 증거로 사용된 경우 예외 발생")
    void deleteEvidence_fail_when_key_evidence() {
        // given
        Evidence evidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(savedScenario.getId())
                .code("EVD_01")
                .title("핵심증거")
                .description("설명")
                .evidenceType(EvidenceType.PHYSICAL)
                .importance(EvidenceImportance.HIGH)
                .sortOrder(1)
                .build());

        solutionRepository.save(Solution.builder()
                .scenarioId(savedScenario.getId())
                .culpritSuspectId(savedSuspect.getId())
                .motive("동기")
                .method("수단")
                .coverUp("은폐")
                .fullExplanation("전체 설명")
                .keyEvidenceIds(evidence.getId().toString())
                .build());

        // when & then
        assertThatThrownBy(() -> customScenarioService.deleteEvidence(OWNER_USER_ID, evidence.getId()))
                .isInstanceOf(ScenarioException.class)
                .hasMessageContaining(ScenarioErrorCode.EVIDENCE_IS_KEY.getMessage());
    }

    @Test
    @DisplayName("증거 삭제 실패 - 다른 증거의 해금 조건으로 사용 중인 경우 예외 발생")
    void deleteEvidence_fail_when_prerequisite() {
        // given
        Evidence evidenceA = evidenceRepository.save(Evidence.builder()
                .scenarioId(savedScenario.getId())
                .code("EVD_A")
                .title("선행 증거")
                .description("설명")
                .evidenceType(EvidenceType.PHYSICAL)
                .importance(EvidenceImportance.NORMAL)
                .sortOrder(1)
                .build());

        Evidence evidenceB = evidenceRepository.save(Evidence.builder()
                .scenarioId(savedScenario.getId())
                .code("EVD_B")
                .title("후행 증거")
                .description("설명")
                .evidenceType(EvidenceType.PHYSICAL)
                .importance(EvidenceImportance.NORMAL)
                .sortOrder(2)
                .build());

        evidenceUnlockRuleRepository.save(EvidenceUnlockRule.builder()
                .scenarioId(savedScenario.getId())
                .evidenceId(evidenceB.getId())
                .evidenceCode(evidenceB.getCode())
                .unlockType(EvidenceUnlockType.EVIDENCE_PRESENTED.name())
                .conditionJson("{\"requiredPresentedEvidenceCode\": \"EVD_A\"}")
                .sortOrder(2)
                .build());

        // when & then
        assertThatThrownBy(() -> customScenarioService.deleteEvidence(OWNER_USER_ID, evidenceA.getId()))
                .isInstanceOf(ScenarioException.class)
                .hasMessageContaining(ScenarioErrorCode.EVIDENCE_IS_PREREQUISITE.getMessage());
    }

    @Test
    @DisplayName("타인의 시나리오 증거 수정/삭제 시나리오 접근 권한 예외 발생")
    void modifyEvidence_fail_unauthorized() {
        // given
        Evidence evidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(savedScenario.getId())
                .code("EVD_01")
                .title("테스트증거")
                .description("설명")
                .evidenceType(EvidenceType.PHYSICAL)
                .importance(EvidenceImportance.NORMAL)
                .sortOrder(1)
                .build());

        CustomEvidenceUpdateRequest request = new CustomEvidenceUpdateRequest();

        // when & then
        assertThatThrownBy(() -> customScenarioService.updateEvidence(OTHER_USER_ID, evidence.getId(), request))
                .isInstanceOf(ScenarioException.class);
    }

    @Test
    @DisplayName("증거 수정 시 unlockType 누락되어도 conditionJson 변경 시 rule 재생성 성공")
    void updateEvidence_rebuildRule_whenConditionChanges() {
        // given
        Evidence evidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(savedScenario.getId())
                .code("EVD_01")
                .title("기존증거")
                .description("설명")
                .evidenceType(EvidenceType.PHYSICAL)
                .importance(EvidenceImportance.NORMAL)
                .unlockType(EvidenceUnlockType.TIME)
                .unlockConditionJson("{\"original\": true}")
                .sortOrder(1)
                .build());

        evidenceUnlockRuleRepository.save(EvidenceUnlockRule.builder()
                .scenarioId(evidence.getScenarioId())
                .evidenceId(evidence.getId())
                .evidenceCode(evidence.getCode())
                .unlockType(evidence.getUnlockType().name())
                .conditionJson(evidence.getUnlockConditionJson())
                .sortOrder(evidence.getSortOrder())
                .build());

        CustomEvidenceUpdateRequest request = new CustomEvidenceUpdateRequest();
        ReflectionTestUtils.setField(request, "title", "변경증거");
        // unlockType은 누락 (null)
        ReflectionTestUtils.setField(request, "unlockConditionJson", "{\"changed\": true}");

        // when
        customScenarioService.updateEvidence(OWNER_USER_ID, evidence.getId(), request);

        // then
        List<EvidenceUnlockRule> rules = evidenceUnlockRuleRepository.findAll();
        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).getConditionJson()).isEqualTo("{\"changed\":true}");
    }

}
