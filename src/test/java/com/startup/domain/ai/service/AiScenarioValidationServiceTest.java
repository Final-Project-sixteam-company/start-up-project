package com.startup.domain.ai.service;

import com.startup.domain.ai.client.AiClient;
import com.startup.domain.ai.client.AiRequestParams;
import com.startup.domain.ai.dto.ScenarioValidationData;
import com.startup.domain.ai.dto.ScenarioValidationResponse;
import com.startup.domain.ai.entity.ScenarioValidationResult;
import com.startup.domain.ai.enums.ValidationStatus;
import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import com.startup.domain.ai.prompt.AiPromptBuilder;
import com.startup.domain.ai.repository.ScenarioValidationResultRepository;
import com.startup.domain.ai.support.MockScenarioDataReader;
import com.startup.domain.ai.support.RuleBasedScenarioValidator;
import com.startup.domain.ai.support.ScenarioDataReader;
import com.startup.infrastructure.redis.lock.LockException;
import com.startup.infrastructure.redis.lock.LockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiScenarioValidationServiceTest {

    private AiPromptBuilder promptBuilder;
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        promptBuilder = new AiPromptBuilder(
                new ClassPathResource("prompts/interrogation_system_prompt.txt"),
                new ClassPathResource("prompts/interrogation_user_prompt.txt"),
                new ClassPathResource("prompts/evidence_interrogation_user_prompt.txt"),
                new ClassPathResource("prompts/final_deduction_scoring_prompt.txt"),
                new ClassPathResource("prompts/scenario_validation_prompt.txt")
        );
        jsonMapper = JsonMapper.builder().build();
    }

    @Test
    @DisplayName("mock mode validate는 checkItems 10개를 반환한다")
    void validate_mockMode_returnsTenCheckItems() {
        SavedRepository repository = new SavedRepository();
        AiClient aiClient = mock(AiClient.class);
        when(aiClient.isMockMode()).thenReturn(true);

        AiScenarioValidationService service = newService(
                new MockScenarioDataReader(), aiClient, repository.mock(), new NoopLockService());

        ScenarioValidationResponse response = service.validate(1L);

        assertThat(response.checkItems()).hasSize(10);
        assertThat(response.validationStatus())
                .isIn(ValidationStatus.PASSED.name(), ValidationStatus.PASSED_WITH_WARNINGS.name());
    }

    @Test
    @DisplayName("getLatestResult는 저장된 최신 결과를 반환한다")
    void getLatestResult_returnsSavedResult() {
        SavedRepository repository = new SavedRepository();
        AiClient aiClient = mock(AiClient.class);
        when(aiClient.isMockMode()).thenReturn(true);
        AiScenarioValidationService service = newService(
                new MockScenarioDataReader(), aiClient, repository.mock(), new NoopLockService());

        ScenarioValidationResponse saved = service.validate(1L);
        ScenarioValidationResponse latest = service.getLatestResult(1L);

        assertThat(latest.scenarioId()).isEqualTo(saved.scenarioId());
        assertThat(latest.validationStatus()).isEqualTo(saved.validationStatus());
        assertThat(latest.checkItems()).hasSize(10);
    }

    @Test
    @DisplayName("AI 실패 시 FAILED 상태와 0점 AI 항목을 반영한다")
    void validate_aiFailure_returnsFailed() {
        SavedRepository repository = new SavedRepository();
        AiClient aiClient = mock(AiClient.class);
        when(aiClient.isMockMode()).thenReturn(false);
        when(aiClient.chat(anyString(), anyString(), any(AiRequestParams.class)))
                .thenThrow(new RuntimeException("boom"));

        AiScenarioValidationService service = newService(
                new MockScenarioDataReader(), aiClient, repository.mock(), new NoopLockService());

        ScenarioValidationResponse response = service.validate(1L);

        assertThat(response.validationStatus()).isEqualTo(ValidationStatus.FAILED.name());
        assertThat(response.validationScore()).isLessThanOrEqualTo(40);
        assertThat(response.checkItems()).hasSize(10);
    }

    @Test
    @DisplayName("AI 응답이 코드펜스로 감싸지고 일부 key가 없어도 0점으로 보완한다")
    void validate_aiResponseWithCodeFenceAndMissingKey_parsesAndCompletesItems() {
        SavedRepository repository = new SavedRepository();
        AiClient aiClient = mock(AiClient.class);
        when(aiClient.isMockMode()).thenReturn(false);
        when(aiClient.chat(anyString(), anyString(), any(AiRequestParams.class)))
                .thenReturn("""
                        ```json
                        {
                          "items": [
                            {"key": "motive_quality", "score": 9, "comment": "충분함"},
                            {"key": "method_quality", "score": 8, "comment": "가능함"},
                            {"key": "fake_evidence_quality", "score": 7, "comment": "적절함"},
                            {"key": "suspect_suspicion", "score": 7, "comment": "적절함"},
                            {"key": "hint_strength", "score": 8, "comment": "적절함"}
                          ],
                          "problemSummary": "대체로 적절합니다.",
                          "suggestion": "해설 완결성을 보강하세요."
                        }
                        ```
                        """);

        AiScenarioValidationService service = newService(
                new MockScenarioDataReader(), aiClient, repository.mock(), new NoopLockService());

        ScenarioValidationResponse response = service.validate(1L);

        assertThat(response.checkItems()).hasSize(10);
        assertThat(response.validationScore()).isEqualTo(79);
        assertThat(response.validationStatus()).isEqualTo(ValidationStatus.PASSED_WITH_WARNINGS.name());
    }

    @Test
    @DisplayName("hard blocker가 있으면 AI 호출 없이 NEEDS_FIX가 된다")
    void validate_hardBlocker_skipsAiCall() {
        SavedRepository repository = new SavedRepository();
        AiClient aiClient = mock(AiClient.class);
        ScenarioDataReader hardBlockerReader = scenarioId -> {
            ScenarioValidationData data = new MockScenarioDataReader().loadForValidation(1L);
            return new ScenarioValidationData(
                    data.scenario(),
                    data.victim(),
                    data.locations(),
                    data.suspects(),
                    data.evidences(),
                    data.hints(),
                    data.timelineEvents(),
                    null,
                    data.solutionEvidences(),
                    data.responsePolicies(),
                    data.suspectSecrets()
            );
        };
        AiScenarioValidationService service = newService(
                hardBlockerReader, aiClient, repository.mock(), new NoopLockService());

        ScenarioValidationResponse response = service.validate(1L);

        assertThat(response.validationStatus()).isEqualTo(ValidationStatus.NEEDS_FIX.name());
        assertThat(response.checkItems()).hasSize(10);
        verify(aiClient, never()).chat(anyString(), anyString(), any(AiRequestParams.class));
    }

    @Test
    @DisplayName("lock 획득 실패 시 SCENARIO_VALIDATION_IN_PROGRESS가 발생한다")
    void validate_lockFailure_throwsInProgress() {
        AiClient aiClient = mock(AiClient.class);
        AiScenarioValidationService service = newService(
                new MockScenarioDataReader(), aiClient, new SavedRepository().mock(), new FailingLockService());

        assertThatThrownBy(() -> service.validate(1L))
                .isInstanceOfSatisfying(AiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.SCENARIO_VALIDATION_IN_PROGRESS));
    }

    private AiScenarioValidationService newService(ScenarioDataReader scenarioDataReader,
                                                  AiClient aiClient,
                                                  ScenarioValidationResultRepository repository,
                                                  LockService lockService) {
        return new AiScenarioValidationService(
                scenarioDataReader,
                new RuleBasedScenarioValidator(),
                promptBuilder,
                aiClient,
                repository,
                lockService,
                jsonMapper
        );
    }

    private static class SavedRepository {

        private final AtomicReference<ScenarioValidationResult> saved = new AtomicReference<>();

        ScenarioValidationResultRepository mock() {
            ScenarioValidationResultRepository repository =
                    org.mockito.Mockito.mock(ScenarioValidationResultRepository.class);
            when(repository.save(any(ScenarioValidationResult.class))).thenAnswer(invocation -> {
                ScenarioValidationResult result = invocation.getArgument(0);
                saved.set(result);
                return result;
            });
            when(repository.findTopByScenarioIdOrderByCheckedAtDescIdDesc(any()))
                    .thenAnswer(invocation -> Optional.ofNullable(saved.get()));
            return repository;
        }
    }

    private static class NoopLockService implements LockService {

        @Override
        public void lock(String key, long waitTime, long leaseTime, TimeUnit timeUnit) {
        }

        @Override
        public void unlock(String key) {
        }
    }

    private static class FailingLockService implements LockService {

        @Override
        public void lock(String key, long waitTime, long leaseTime, TimeUnit timeUnit) {
            throw new LockException("failed");
        }

        @Override
        public void unlock(String key) {
        }
    }
}
