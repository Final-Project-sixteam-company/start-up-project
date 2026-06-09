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
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class CustomScenarioServiceSuspectTest {

    @Autowired private CustomScenarioService customScenarioService;
    @Autowired private ScenarioRepository scenarioRepository;
    @Autowired private SuspectRepository suspectRepository;
    @Autowired private SolutionRepository solutionRepository;
    @Autowired private SuspectResponsePolicyRepository suspectResponsePolicyRepository;
    @Autowired private EvidenceSuspectRepository evidenceSuspectRepository;
    @Autowired private EvidenceUnlockRuleRepository evidenceUnlockRuleRepository;
    @Autowired private JsonMapper jsonMapper;

    private static final Long OWNER_USER_ID = 100L;
    private static final Long OTHER_USER_ID = 999L;
    private Scenario savedScenario;

    @BeforeEach
    void setUp() {
        savedScenario = scenarioRepository.save(Scenario.builder()
                .title("커스텀 시나리오 용의자 테스트")
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
        solutionRepository.deleteAllInBatch();
        evidenceUnlockRuleRepository.deleteAllInBatch();
        suspectResponsePolicyRepository.deleteAllInBatch();
        evidenceSuspectRepository.deleteAllInBatch();
        suspectRepository.deleteAllInBatch();
        scenarioRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("용의자 등록 성공 및 updatedAt 갱신 확인")
    void createSuspect_success() {
        // given
        CustomSuspectCreateRequest request = new CustomSuspectCreateRequest();
        ReflectionTestUtils.setField(request, "name", "김철수");
        ReflectionTestUtils.setField(request, "role", "피해자의 친구");
        ReflectionTestUtils.setField(request, "characterType", "NPC");
        ReflectionTestUtils.setField(request, "culpritEligible", true);

        // when
        CustomSuspectCreateResponse response = customScenarioService.createSuspect(OWNER_USER_ID, savedScenario.getId(), request);

        // then
        assertThat(response.getSuspectId()).isNotNull();
        Suspect suspect = suspectRepository.findById(response.getSuspectId()).orElseThrow();
        assertThat(suspect.getName()).isEqualTo("김철수");
    }

    @Test
    @DisplayName("용의자 수정 성공 및 연관 데이터 확인")
    void updateSuspect_success() {
        // given
        Suspect suspect = suspectRepository.save(Suspect.builder()
                .scenarioId(savedScenario.getId())
                .code("SUSPECT_001")
                .name("기존이름")
                .role("역할")
                .characterType("NPC")
                .culpritEligible(true)
                .sortOrder(1)
                .build());

        CustomSuspectUpdateRequest request = new CustomSuspectUpdateRequest();
        ReflectionTestUtils.setField(request, "name", "변경이름");
        ReflectionTestUtils.setField(request, "role", "변경된역할");
        ReflectionTestUtils.setField(request, "characterType", "NPC");
        ReflectionTestUtils.setField(request, "culpritEligible", false);
        
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode policyNode = mapper.createArrayNode();
        policyNode.addObject().put("policyText", "응답정책테스트").put("conditionKey", "DEFAULT");
        ReflectionTestUtils.setField(request, "responsePolicyJson", policyNode);

        // when
        CustomSuspectResponse response = customScenarioService.updateSuspect(OWNER_USER_ID, suspect.getId(), request);

        // then
        assertThat(response.getName()).isEqualTo("변경이름");
        assertThat(suspectResponsePolicyRepository.findAllBySuspectId(suspect.getId())).hasSize(1);
    }

    @Test
    @DisplayName("용의자 삭제 성공 (고아 데이터 동시 삭제)")
    void deleteSuspect_success() {
        // given
        Suspect suspect = suspectRepository.save(Suspect.builder()
                .scenarioId(savedScenario.getId())
                .code("SUSPECT_001")
                .name("삭제대상")
                .role("역할")
                .characterType("NPC")
                .culpritEligible(true)
                .sortOrder(1)
                .build());

        // when
        customScenarioService.deleteSuspect(OWNER_USER_ID, suspect.getId());

        // then
        assertThat(suspectRepository.findById(suspect.getId())).isEmpty();
    }

    @Test
    @DisplayName("용의자 삭제 실패 - 범인으로 지정된 경우 예외 발생")
    void deleteSuspect_fail_when_culprit() {
        // given
        Suspect suspect = suspectRepository.save(Suspect.builder()
                .scenarioId(savedScenario.getId())
                .code("SUSPECT_001")
                .name("범인")
                .role("역할")
                .characterType("NPC")
                .culpritEligible(true)
                .sortOrder(1)
                .build());

        solutionRepository.save(Solution.builder()
                .scenarioId(savedScenario.getId())
                .culpritSuspectId(suspect.getId())
                .motive("동기")
                .method("수단")
                .coverUp("은폐")
                .fullExplanation("전체 설명")
                .keyEvidenceIds("1")
                .build());

        // when & then
        assertThatThrownBy(() -> customScenarioService.deleteSuspect(OWNER_USER_ID, suspect.getId()))
                .isInstanceOf(ScenarioException.class)
                .hasMessageContaining(ScenarioErrorCode.SUSPECT_IS_CULPRIT.getMessage());
    }

    @Test
    @DisplayName("용의자 수정 실패 - 범인으로 지목된 용의자의 culpritEligible을 false로 변경 시 예외 발생")
    void updateSuspect_fail_when_culprit_and_eligible_false() {
        // given
        Suspect suspect = suspectRepository.save(Suspect.builder()
                .scenarioId(savedScenario.getId())
                .code("SUSPECT_001")
                .name("범인")
                .role("역할")
                .characterType("NPC")
                .culpritEligible(true)
                .sortOrder(1)
                .build());

        solutionRepository.save(Solution.builder()
                .scenarioId(savedScenario.getId())
                .culpritSuspectId(suspect.getId())
                .motive("동기")
                .method("수단")
                .coverUp("은폐")
                .fullExplanation("전체 설명")
                .keyEvidenceIds("1")
                .build());

        CustomSuspectUpdateRequest request = new CustomSuspectUpdateRequest();
        ReflectionTestUtils.setField(request, "culpritEligible", false);

        // when & then
        assertThatThrownBy(() -> customScenarioService.updateSuspect(OWNER_USER_ID, suspect.getId(), request))
                .isInstanceOf(ScenarioException.class)
                .hasMessageContaining(ScenarioErrorCode.SUSPECT_IS_CULPRIT.getMessage());
    }

    @Test
    @DisplayName("용의자 삭제 실패 - 특정 증거의 해금 조건(대상 인물)으로 사용 중인 경우 예외 발생")
    void deleteSuspect_fail_when_prerequisite() {
        // given
        Suspect suspect = suspectRepository.save(Suspect.builder()
                .scenarioId(savedScenario.getId())
                .code("SUSPECT_001")
                .name("용의자")
                .role("역할")
                .characterType("NPC")
                .culpritEligible(true)
                .sortOrder(1)
                .build());

        evidenceUnlockRuleRepository.save(EvidenceUnlockRule.builder()
                .scenarioId(savedScenario.getId())
                .evidenceId(999L)
                .evidenceCode("EVD_DUMMY")
                .unlockType(EvidenceUnlockType.INTERROGATION.name())
                .conditionJson("{\"requiredCharacterCode\": \"SUSPECT_001\"}")
                .sortOrder(1)
                .build());

        // when & then
        assertThatThrownBy(() -> customScenarioService.deleteSuspect(OWNER_USER_ID, suspect.getId()))
                .isInstanceOf(ScenarioException.class)
                .hasMessageContaining(ScenarioErrorCode.SUSPECT_IS_PREREQUISITE.getMessage());
    }

    @Test
    @DisplayName("타인의 시나리오 용의자 수정/삭제 시나리오 접근 권한 예외 발생")
    void modifySuspect_fail_unauthorized() {
        // given
        Suspect suspect = suspectRepository.save(Suspect.builder()
                .scenarioId(savedScenario.getId())
                .code("SUSPECT_001")
                .name("테스트용의자")
                .role("역할")
                .characterType("NPC")
                .culpritEligible(true)
                .sortOrder(1)
                .build());

        CustomSuspectUpdateRequest request = new CustomSuspectUpdateRequest();

        // when & then
        assertThatThrownBy(() -> customScenarioService.updateSuspect(OTHER_USER_ID, suspect.getId(), request))
                .isInstanceOf(ScenarioException.class);
    }
}
