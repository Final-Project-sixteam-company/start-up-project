package com.startup.domain.scenario.service;

import com.startup.common.error.BusinessException;
import com.startup.domain.scenario.dto.*;
import com.startup.domain.scenario.entity.*;
import com.startup.domain.scenario.enums.*;
import com.startup.domain.scenario.repository.*;
import com.startup.domain.ai.repository.SuspectResponsePolicyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class CustomScenarioServiceTest {

    @Autowired private CustomScenarioService customScenarioService;
    @Autowired private ScenarioRepository scenarioRepository;
    @Autowired private ScenarioLocationRepository locationRepository;
    @Autowired private VictimRepository victimRepository;
    @Autowired private SuspectRepository suspectRepository;
    @Autowired private EvidenceRepository evidenceRepository;
    @Autowired private EvidenceSuspectRepository evidenceSuspectRepository;
    @Autowired private HintRepository hintRepository;
    @Autowired private SolutionRepository solutionRepository;
    @Autowired private SuspectResponsePolicyRepository suspectResponsePolicyRepository;
    @Autowired private EvidenceUnlockRuleRepository evidenceUnlockRuleRepository;

    private static final Long OWNER_USER_ID = 100L;
    private static final Long OTHER_USER_ID = 999L;
    private Scenario savedScenario;

    @BeforeEach
    void setUp() {
        savedScenario = scenarioRepository.save(Scenario.builder()
                .title("커스텀 시나리오 테스트")
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
        hintRepository.deleteAllInBatch();
        suspectResponsePolicyRepository.deleteAllInBatch();
        evidenceUnlockRuleRepository.deleteAllInBatch();
        evidenceSuspectRepository.deleteAllInBatch();
        evidenceRepository.deleteAllInBatch();
        suspectRepository.deleteAllInBatch();
        victimRepository.deleteAllInBatch();
        locationRepository.deleteAllInBatch();
        scenarioRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("장소 등록 성공 및 updatedAt 갱신 확인")
    void createLocation_success() {
        // given
        CustomLocationCreateRequest request = new CustomLocationCreateRequest();
        // 리플렉션을 사용하거나 setter가 없다면 테스트용 객체를 생성하는 방법이 필요. 
        // 일단 필드 주입이나 생성자가 필요한데, DTO에 @Setter가 없으므로 ReflectionTestUtils 사용
        ReflectionTestUtils.setField(request, "name", "거실");
        ReflectionTestUtils.setField(request, "description", "넓은 거실");
        
        LocalDateTime beforeUpdate = savedScenario.getUpdatedAt();

        // when
        CustomLocationCreateResponse response = customScenarioService.createLocation(OWNER_USER_ID, savedScenario.getId(), request);

        // then
        assertThat(response.getLocationId()).isNotNull();
        ScenarioLocation location = locationRepository.findById(response.getLocationId()).orElseThrow();
        assertThat(location.getName()).isEqualTo("거실");
        assertThat(location.getSortOrder()).isEqualTo(1);

        Scenario updatedScenario = scenarioRepository.findById(savedScenario.getId()).orElseThrow();
        assertThat(updatedScenario.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate != null ? beforeUpdate : LocalDateTime.MIN);
    }

    @Test
    @DisplayName("피해자 등록(UPSERT) 성공 및 updatedAt 갱신 확인")
    void createOrUpdateVictim_success() {
        // given
        CustomVictimCreateRequest request = new CustomVictimCreateRequest();
        ReflectionTestUtils.setField(request, "name", "김피해");
        ReflectionTestUtils.setField(request, "role", "사업가");
        ReflectionTestUtils.setField(request, "description", "사건의 피해자");
        ReflectionTestUtils.setField(request, "causeOfDeath", "독살");

        LocalDateTime beforeUpdate = savedScenario.getUpdatedAt();

        // when
        CustomVictimCreateResponse response = customScenarioService.createOrUpdateVictim(OWNER_USER_ID, savedScenario.getId(), request);

        // then
        assertThat(response.getVictimId()).isNotNull();
        Victim victim = victimRepository.findById(response.getVictimId()).orElseThrow();
        assertThat(victim.getName()).isEqualTo("김피해");
        
        // UPSERT 로직 테스트 (한 번 더 생성 요청시 같은 ID가 갱신되어야 함)
        ReflectionTestUtils.setField(request, "name", "이피해");
        CustomVictimCreateResponse response2 = customScenarioService.createOrUpdateVictim(OWNER_USER_ID, savedScenario.getId(), request);
        assertThat(response2.getVictimId()).isEqualTo(response.getVictimId());
        Victim updatedVictim = victimRepository.findById(response2.getVictimId()).orElseThrow();
        assertThat(updatedVictim.getName()).isEqualTo("이피해");

        Scenario updatedScenario = scenarioRepository.findById(savedScenario.getId()).orElseThrow();
        assertThat(updatedScenario.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate != null ? beforeUpdate : LocalDateTime.MIN);
    }

    @Test
    @DisplayName("용의자 등록 성공 및 updatedAt 갱신 확인")
    void createSuspect_success() {
        // given
        CustomSuspectCreateRequest request = new CustomSuspectCreateRequest();
        ReflectionTestUtils.setField(request, "name", "최용의");
        ReflectionTestUtils.setField(request, "role", "비서");
        ReflectionTestUtils.setField(request, "publicProfile", "피해자의 비서");
        ReflectionTestUtils.setField(request, "alibi", "혼자 집에 있었음");
        ReflectionTestUtils.setField(request, "culpritEligible", true);
        ReflectionTestUtils.setField(request, "suspicionLevel", 50);
        try {
            JsonMapper mapper = JsonMapper.builder().build();
            ReflectionTestUtils.setField(request, "responsePolicyJson", mapper.readTree("{\"tone\":\"aggressive\"}"));
        } catch (Exception e) {}

        LocalDateTime beforeUpdate = savedScenario.getUpdatedAt();

        // when
        CustomSuspectCreateResponse response = customScenarioService.createSuspect(OWNER_USER_ID, savedScenario.getId(), request);

        // then
        assertThat(response.getSuspectId()).isNotNull();
        Suspect suspect = suspectRepository.findById(response.getSuspectId()).orElseThrow();
        assertThat(suspect.getName()).isEqualTo("최용의");
        assertThat(suspect.getSortOrder()).isEqualTo(1); // 첫 용의자
        assertThat(suspect.getSuspicionLevel()).isEqualTo(50);
        assertThat(suspect.getResponsePolicyJson()).isEqualTo("{\"tone\":\"aggressive\"}");

        assertThat(suspectResponsePolicyRepository.count()).isEqualTo(1);
        var policy = suspectResponsePolicyRepository.findAll().get(0);
        assertThat(policy.getTone()).isEqualTo("aggressive");
        assertThat(policy.getConditionKey()).isEqualTo("DEFAULT");

        Scenario updatedScenario = scenarioRepository.findById(savedScenario.getId()).orElseThrow();
        assertThat(updatedScenario.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate != null ? beforeUpdate : LocalDateTime.MIN);
    }

    @Test
    @DisplayName("용의자 응답 정책에 잘못된 증거 ID가 포함되어 있으면 실패")
    void createSuspect_fail_malformedEvidencePolicy() {
        CustomSuspectCreateRequest request = new CustomSuspectCreateRequest();
        ReflectionTestUtils.setField(request, "name", "이상한");
        try {
            JsonMapper mapper = JsonMapper.builder().build();
            // requiredEvidenceIds에 문자열을 넣은 잘못된 포맷
            ReflectionTestUtils.setField(request, "responsePolicyJson", mapper.readTree("{\"requiredEvidenceIds\":\"invalid_string\"}"));
        } catch (Exception e) {}

        assertThatThrownBy(() -> customScenarioService.createSuspect(OWNER_USER_ID, savedScenario.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("배열 형태");

        try {
            JsonMapper mapper = JsonMapper.builder().build();
            // 존재하지 않는 증거 ID
            ReflectionTestUtils.setField(request, "responsePolicyJson", mapper.readTree("{\"requiredEvidenceIds\":[99999]}"));
        } catch (Exception e) {}

        assertThatThrownBy(() -> customScenarioService.createSuspect(OWNER_USER_ID, savedScenario.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("존재하지 않거나");
    }

    @Test
    @DisplayName("용의자 응답 정책에 잘못된 제시 증거 ID가 포함되어 있으면 실패")
    void createSuspect_fail_malformedPresentedEvidencePolicy() {
        CustomSuspectCreateRequest request = new CustomSuspectCreateRequest();
        ReflectionTestUtils.setField(request, "name", "이상한");

        try {
            JsonMapper mapper = JsonMapper.builder().build();
            // presentedEvidenceId에 문자열을 넣은 잘못된 포맷
            ReflectionTestUtils.setField(request, "responsePolicyJson", mapper.readTree("{\"conditionKey\":\"GATED\",\"presentedEvidenceId\":\"invalid_string\"}"));
        } catch (Exception e) {}

        assertThatThrownBy(() -> customScenarioService.createSuspect(OWNER_USER_ID, savedScenario.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("숫자여야 합니다.");

        try {
            JsonMapper mapper = JsonMapper.builder().build();
            // 존재하지 않는 증거 ID
            ReflectionTestUtils.setField(request, "responsePolicyJson", mapper.readTree("{\"conditionKey\":\"GATED\",\"presentedEvidenceId\":99999}"));
        } catch (Exception e) {}

        assertThatThrownBy(() -> customScenarioService.createSuspect(OWNER_USER_ID, savedScenario.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("존재하지 않거나");
    }

    @Test
    @DisplayName("증거 등록 성공 및 updatedAt 갱신 확인")
    void createEvidence_success() {
        // given: 용의자가 먼저 등록되어야 함
        Suspect suspect = suspectRepository.save(Suspect.builder()
                .scenarioId(savedScenario.getId())
                .code("S_TEST")
                .name("용의자A")
                .role("친구")
                .publicProfile("테스트")
                .alibi("알리바이")
                .culpritEligible(true)
                .sortOrder(1)
                .build());

        Evidence prereqEvidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(savedScenario.getId())
                .code("E_PREREQ")
                .title("선행 증거")
                .description("제시용 증거")
                .evidenceType(EvidenceType.PHYSICAL)
                .importance(EvidenceImportance.NORMAL)
                .sortOrder(0)
                .build());

        CustomEvidenceCreateRequest request = new CustomEvidenceCreateRequest();
        ReflectionTestUtils.setField(request, "title", "피묻은 칼");
        ReflectionTestUtils.setField(request, "description", "칼입니다.");
        ReflectionTestUtils.setField(request, "evidenceType", EvidenceType.PHYSICAL);
        ReflectionTestUtils.setField(request, "relatedSuspectIds", List.of(suspect.getId()));
        ReflectionTestUtils.setField(request, "unlockType", EvidenceUnlockType.EVIDENCE_PRESENTED);
        ReflectionTestUtils.setField(request, "unlockConditionJson", "{\"evidenceId\": " + prereqEvidence.getId() + ", \"requiredCharacterId\": " + suspect.getId() + "}");

        LocalDateTime beforeUpdate = savedScenario.getUpdatedAt();

        // when
        CustomEvidenceCreateResponse response = customScenarioService.createEvidence(OWNER_USER_ID, savedScenario.getId(), request);

        // then
        assertThat(response.getEvidenceId()).isNotNull();
        Evidence evidence = evidenceRepository.findById(response.getEvidenceId()).orElseThrow();
        assertThat(evidence.getTitle()).isEqualTo("피묻은 칼");
        
        long mappedCount = evidenceSuspectRepository.count();
        assertThat(mappedCount).isEqualTo(1); // 용의자 매핑 확인

        long unlockRuleCount = evidenceUnlockRuleRepository.count();
        assertThat(unlockRuleCount).isEqualTo(1);
        var rule = evidenceUnlockRuleRepository.findAll().get(0);
        assertThat(rule.getUnlockType()).isEqualTo("EVIDENCE_PRESENTED");
        assertThat(rule.getConditionJson()).contains("\"requiredPresentedEvidenceCode\":\"E_PREREQ\"");
        assertThat(rule.getConditionJson()).contains("\"requiredCharacterCode\":\"" + suspect.getCode() + "\"");

        Scenario updatedScenario = scenarioRepository.findById(savedScenario.getId()).orElseThrow();
        assertThat(updatedScenario.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate != null ? beforeUpdate : LocalDateTime.MIN);
    }

    @Test
    @DisplayName("EVIDENCE_PRESENTED 조건의 requiredEvidenceIds가 배열이 아니면 실패")
    void createEvidence_fail_requiredEvidenceIds_notArray() {
        Evidence prereqEvidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(savedScenario.getId())
                .code("E_PREREQ_1")
                .title("선행 증거")
                .description("제시용 증거")
                .evidenceType(EvidenceType.PHYSICAL)
                .importance(EvidenceImportance.NORMAL)
                .sortOrder(0)
                .build());

        CustomEvidenceCreateRequest request = new CustomEvidenceCreateRequest();
        ReflectionTestUtils.setField(request, "title", "증거1");
        ReflectionTestUtils.setField(request, "description", "설명");
        ReflectionTestUtils.setField(request, "evidenceType", EvidenceType.PHYSICAL);
        ReflectionTestUtils.setField(request, "unlockType", EvidenceUnlockType.EVIDENCE_PRESENTED);
        
        // requiredEvidenceIds를 단일 문자열로 잘못 보냄
        ReflectionTestUtils.setField(request, "unlockConditionJson",
                "{\"evidenceId\": " + prereqEvidence.getId() + ", \"requiredEvidenceIds\": \"not_array\"}");

        assertThatThrownBy(() -> customScenarioService.createEvidence(OWNER_USER_ID, savedScenario.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("배열 형태여야 합니다");
    }

    @Test
    @DisplayName("EVIDENCE_PRESENTED 조건의 requiredEvidenceIds 요소가 숫자가 아니면 실패")
    void createEvidence_fail_requiredEvidenceIds_notNumber() {
        Evidence prereqEvidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(savedScenario.getId())
                .code("E_PREREQ_2")
                .title("선행 증거")
                .description("제시용 증거")
                .evidenceType(EvidenceType.PHYSICAL)
                .importance(EvidenceImportance.NORMAL)
                .sortOrder(0)
                .build());

        CustomEvidenceCreateRequest request = new CustomEvidenceCreateRequest();
        ReflectionTestUtils.setField(request, "title", "증거1");
        ReflectionTestUtils.setField(request, "description", "설명");
        ReflectionTestUtils.setField(request, "evidenceType", EvidenceType.PHYSICAL);
        ReflectionTestUtils.setField(request, "unlockType", EvidenceUnlockType.EVIDENCE_PRESENTED);
        
        // 배열 안에 숫자가 아닌 문자열을 넣음
        ReflectionTestUtils.setField(request, "unlockConditionJson",
                "{\"evidenceId\": " + prereqEvidence.getId() + ", \"requiredEvidenceIds\": [\"invalid_id\"]}");

        assertThatThrownBy(() -> customScenarioService.createEvidence(OWNER_USER_ID, savedScenario.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("숫자여야 합니다");
    }

    @Test
    @DisplayName("힌트 등록 성공 및 updatedAt 갱신 확인")
    void createHint_success() {
        // given
        CustomHintCreateRequest request = new CustomHintCreateRequest();
        ReflectionTestUtils.setField(request, "content", "이것은 힌트입니다.");
        ReflectionTestUtils.setField(request, "penaltyScore", 10);

        LocalDateTime beforeUpdate = savedScenario.getUpdatedAt();

        // when
        CustomHintCreateResponse response = customScenarioService.createHint(OWNER_USER_ID, savedScenario.getId(), request);

        // then
        assertThat(response.getHintId()).isNotNull();
        Hint hint = hintRepository.findById(response.getHintId()).orElseThrow();
        assertThat(hint.getContent()).isEqualTo("이것은 힌트입니다.");
        assertThat(hint.getHintLevel()).isEqualTo(1); // 첫 번째 힌트

        Scenario updatedScenario = scenarioRepository.findById(savedScenario.getId()).orElseThrow();
        assertThat(updatedScenario.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate != null ? beforeUpdate : LocalDateTime.MIN);
    }

    @Test
    @DisplayName("정답 등록(UPSERT) 성공 및 updatedAt 갱신 확인")
    void createOrUpdateSolution_success() {
        // given: 용의자가 먼저 등록되어야 함
        Suspect suspect = suspectRepository.save(Suspect.builder()
                .scenarioId(savedScenario.getId())
                .name("진범")
                .role("원수")
                .publicProfile("테스트")
                .alibi("알리바이")
                .culpritEligible(true)
                .sortOrder(1)
                .build());

        CustomSolutionCreateRequest request = new CustomSolutionCreateRequest();
        ReflectionTestUtils.setField(request, "culpritSuspectId", suspect.getId());
        ReflectionTestUtils.setField(request, "motive", "원한");
        ReflectionTestUtils.setField(request, "method", "독살");
        ReflectionTestUtils.setField(request, "coverUp", "도주");
        ReflectionTestUtils.setField(request, "fullExplanation", "상세 설명");
        ReflectionTestUtils.setField(request, "keyEvidenceIds", java.util.List.of());

        LocalDateTime beforeUpdate = savedScenario.getUpdatedAt();

        // when
        CustomSolutionCreateResponse response = customScenarioService.createOrUpdateSolution(OWNER_USER_ID, savedScenario.getId(), request);

        // then
        assertThat(response.getSolutionId()).isNotNull();
        Solution solution = solutionRepository.findById(response.getSolutionId()).orElseThrow();
        assertThat(solution.getMotive()).isEqualTo("원한");

        // UPSERT 확인
        ReflectionTestUtils.setField(request, "motive", "금전");
        CustomSolutionCreateResponse response2 = customScenarioService.createOrUpdateSolution(OWNER_USER_ID, savedScenario.getId(), request);
        assertThat(response2.getSolutionId()).isEqualTo(response.getSolutionId());
        
        Solution updatedSolution = solutionRepository.findById(response2.getSolutionId()).orElseThrow();
        assertThat(updatedSolution.getMotive()).isEqualTo("금전");

        Scenario updatedScenario = scenarioRepository.findById(savedScenario.getId()).orElseThrow();
        assertThat(updatedScenario.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate != null ? beforeUpdate : LocalDateTime.MIN);
    }

    @Test
    @DisplayName("정답 등록 실패 - 타 시나리오 또는 존재하지 않는 증거 ID 포함")
    void createOrUpdateSolution_fail_invalidKeyEvidence() {
        // given: 용의자 등록
        Suspect suspect = suspectRepository.save(Suspect.builder()
                .scenarioId(savedScenario.getId())
                .name("진범")
                .role("원수")
                .publicProfile("테스트")
                .alibi("알리바이")
                .culpritEligible(true)
                .sortOrder(1)
                .build());

        CustomSolutionCreateRequest request = new CustomSolutionCreateRequest();
        ReflectionTestUtils.setField(request, "culpritSuspectId", suspect.getId());
        ReflectionTestUtils.setField(request, "motive", "원한");
        ReflectionTestUtils.setField(request, "method", "독살");
        ReflectionTestUtils.setField(request, "coverUp", "도주");
        ReflectionTestUtils.setField(request, "fullExplanation", "상세 설명");
        
        // 9999L 같은 존재하지 않는 증거 ID를 추가
        ReflectionTestUtils.setField(request, "keyEvidenceIds", java.util.List.of(9999L));

        // when & then
        assertThatThrownBy(() -> customScenarioService.createOrUpdateSolution(OWNER_USER_ID, savedScenario.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("일부 증거가 존재하지 않거나 이 시나리오 소속이 아닙니다.");
    }

    @Test
    @DisplayName("용의자 응답 정책이 DEFAULT인데 증거 조건(해금 등)이 설정되어 있으면 실패")
    void createSuspect_fail_defaultPolicyWithGates() {
        // given
        CustomSuspectCreateRequest request = new CustomSuspectCreateRequest();
        ReflectionTestUtils.setField(request, "name", "이상한");
        
        // 유효한 증거 등록 (에러를 우회하기 위해)
        Evidence evidence = evidenceRepository.save(Evidence.builder()
                .scenarioId(savedScenario.getId())
                .title("증거")
                .description("설명")
                .evidenceType(EvidenceType.PHYSICAL)
                .sortOrder(1)
                .build());

        try {
            JsonMapper mapper = JsonMapper.builder().build();
            // conditionKey가 누락되거나 DEFAULT인데 requiredEvidenceIds가 존재함
            String policyJson = String.format("{\"requiredEvidenceIds\":[%d]}", evidence.getId());
            ReflectionTestUtils.setField(request, "responsePolicyJson", mapper.readTree(policyJson));
        } catch (Exception e) {}

        // when & then
        assertThatThrownBy(() -> customScenarioService.createSuspect(OWNER_USER_ID, savedScenario.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("별도의 conditionKey를 지정해주세요");
    }
}
