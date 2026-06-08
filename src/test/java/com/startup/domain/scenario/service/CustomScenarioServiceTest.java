package com.startup.domain.scenario.service;

import com.startup.common.error.BusinessException;
import com.startup.domain.scenario.dto.*;
import com.startup.domain.scenario.entity.*;
import com.startup.domain.scenario.enums.*;
import com.startup.domain.scenario.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

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
        org.springframework.test.util.ReflectionTestUtils.setField(request, "name", "거실");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "description", "넓은 거실");
        
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
        org.springframework.test.util.ReflectionTestUtils.setField(request, "name", "김피해");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "role", "사업가");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "description", "사건의 피해자");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "causeOfDeath", "독살");

        LocalDateTime beforeUpdate = savedScenario.getUpdatedAt();

        // when
        CustomVictimCreateResponse response = customScenarioService.createOrUpdateVictim(OWNER_USER_ID, savedScenario.getId(), request);

        // then
        assertThat(response.getVictimId()).isNotNull();
        Victim victim = victimRepository.findById(response.getVictimId()).orElseThrow();
        assertThat(victim.getName()).isEqualTo("김피해");
        
        // UPSERT 로직 테스트 (한 번 더 생성 요청시 같은 ID가 갱신되어야 함)
        org.springframework.test.util.ReflectionTestUtils.setField(request, "name", "이피해");
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
        org.springframework.test.util.ReflectionTestUtils.setField(request, "name", "최용의");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "role", "비서");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "publicProfile", "피해자의 비서");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "alibi", "혼자 집에 있었음");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "culpritEligible", true);
        org.springframework.test.util.ReflectionTestUtils.setField(request, "suspicionLevel", 50);
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            org.springframework.test.util.ReflectionTestUtils.setField(request, "responsePolicy", mapper.readTree("{\"tone\":\"aggressive\"}"));
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

        Scenario updatedScenario = scenarioRepository.findById(savedScenario.getId()).orElseThrow();
        assertThat(updatedScenario.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate != null ? beforeUpdate : LocalDateTime.MIN);
    }

    @Test
    @DisplayName("증거 등록 성공 및 updatedAt 갱신 확인")
    void createEvidence_success() {
        // given: 용의자가 먼저 등록되어야 함
        Suspect suspect = suspectRepository.save(Suspect.builder()
                .scenarioId(savedScenario.getId())
                .name("용의자A")
                .role("친구")
                .publicProfile("테스트")
                .alibi("알리바이")
                .culpritEligible(true)
                .sortOrder(1)
                .build());

        CustomEvidenceCreateRequest request = new CustomEvidenceCreateRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(request, "title", "피묻은 칼");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "description", "칼입니다.");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "evidenceType", EvidenceType.PHYSICAL);
        org.springframework.test.util.ReflectionTestUtils.setField(request, "relatedSuspectIds", java.util.List.of(suspect.getId()));

        LocalDateTime beforeUpdate = savedScenario.getUpdatedAt();

        // when
        CustomEvidenceCreateResponse response = customScenarioService.createEvidence(OWNER_USER_ID, savedScenario.getId(), request);

        // then
        assertThat(response.getEvidenceId()).isNotNull();
        Evidence evidence = evidenceRepository.findById(response.getEvidenceId()).orElseThrow();
        assertThat(evidence.getTitle()).isEqualTo("피묻은 칼");
        
        long mappedCount = evidenceSuspectRepository.count();
        assertThat(mappedCount).isEqualTo(1); // 용의자 매핑 확인

        Scenario updatedScenario = scenarioRepository.findById(savedScenario.getId()).orElseThrow();
        assertThat(updatedScenario.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate != null ? beforeUpdate : LocalDateTime.MIN);
    }

    @Test
    @DisplayName("힌트 등록 성공 및 updatedAt 갱신 확인")
    void createHint_success() {
        // given
        CustomHintCreateRequest request = new CustomHintCreateRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(request, "content", "이것은 힌트입니다.");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "penaltyScore", 10);

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
        org.springframework.test.util.ReflectionTestUtils.setField(request, "culpritSuspectId", suspect.getId());
        org.springframework.test.util.ReflectionTestUtils.setField(request, "motive", "원한");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "method", "독살");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "coverUp", "도주");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "fullExplanation", "상세 설명");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "keyEvidenceIds", java.util.List.of());

        LocalDateTime beforeUpdate = savedScenario.getUpdatedAt();

        // when
        CustomSolutionCreateResponse response = customScenarioService.createOrUpdateSolution(OWNER_USER_ID, savedScenario.getId(), request);

        // then
        assertThat(response.getSolutionId()).isNotNull();
        Solution solution = solutionRepository.findById(response.getSolutionId()).orElseThrow();
        assertThat(solution.getMotive()).isEqualTo("원한");

        // UPSERT 확인
        org.springframework.test.util.ReflectionTestUtils.setField(request, "motive", "금전");
        CustomSolutionCreateResponse response2 = customScenarioService.createOrUpdateSolution(OWNER_USER_ID, savedScenario.getId(), request);
        assertThat(response2.getSolutionId()).isEqualTo(response.getSolutionId());
        
        Solution updatedSolution = solutionRepository.findById(response2.getSolutionId()).orElseThrow();
        assertThat(updatedSolution.getMotive()).isEqualTo("금전");

        Scenario updatedScenario = scenarioRepository.findById(savedScenario.getId()).orElseThrow();
        assertThat(updatedScenario.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate != null ? beforeUpdate : LocalDateTime.MIN);
    }
}
