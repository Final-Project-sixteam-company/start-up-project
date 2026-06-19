package com.startup.domain.ai.service;

import com.startup.domain.ai.client.AiCallContext;
import com.startup.domain.ai.client.AiCallResult;
import com.startup.domain.ai.client.AiClient;
import com.startup.domain.ai.client.AiRequestParams;
import com.startup.domain.ai.dto.AiValidationOutcome;
import com.startup.domain.ai.dto.AiValidationResult;
import com.startup.domain.ai.dto.ScenarioValidationData;
import com.startup.domain.ai.dto.ScenarioValidationResponse;
import com.startup.domain.ai.dto.ValidationCheckItem;
import com.startup.domain.ai.entity.ScenarioValidationResult;
import com.startup.domain.ai.enums.AiFeatureType;
import com.startup.domain.ai.enums.ValidationSeverity;
import com.startup.domain.ai.enums.ValidationSource;
import com.startup.domain.ai.enums.ValidationStatus;
import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import com.startup.domain.ai.prompt.AiPromptBuilder;
import com.startup.domain.ai.repository.ScenarioValidationResultRepository;
import com.startup.domain.ai.support.RuleBasedScenarioValidator;
import com.startup.domain.ai.support.ScenarioDataReader;
import com.startup.infrastructure.redis.lock.LockException;
import com.startup.infrastructure.redis.lock.LockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiScenarioValidationService {

    private static final String LOCK_PREFIX = "scenario-validation:";
    private static final String PROMPT_VERSION = "scenario_validation_v1";
    private static final long LOCK_WAIT_SECONDS = 3;
    private static final long LOCK_LEASE_SECONDS = 120;

    private static final Map<String, String> AI_CHECK_NAMES = Map.of(
            "motive_quality", "범행 동기 납득 가능성",
            "method_quality", "범행 방법 가능성",
            "fake_evidence_quality", "페이크 단서 품질",
            "suspect_suspicion", "무고한 용의자 수상함 정도",
            "hint_strength", "힌트 강도 적절성",
            "explanation_completeness", "최종 해설 완결성"
    );

    private static final List<String> REQUIRED_AI_KEYS = List.of(
            "motive_quality",
            "method_quality",
            "fake_evidence_quality",
            "suspect_suspicion",
            "hint_strength",
            "explanation_completeness"
    );

    private static final List<String> PUBLIC_CHECK_ORDER = List.of(
            "culprit_set",
            "motive_quality",
            "method_quality",
            "key_evidence_exist",
            "fake_evidence_quality",
            "all_alibi",
            "suspect_suspicion",
            "hint_strength",
            "explanation_completeness",
            "all_response_policy"
    );

    private final ScenarioDataReader scenarioDataReader;
    private final RuleBasedScenarioValidator ruleValidator;
    private final AiPromptBuilder promptBuilder;
    private final AiClient aiClient;
    private final ScenarioValidationResultRepository resultRepository;
    private final LockService lockService;
    private final JsonMapper jsonMapper;

    @Value("${caselab.ai.validation.temperature:0.2}")
    private double temperature;

    @Value("${caselab.ai.validation.max-tokens:1500}")
    private int maxTokens;

    public ScenarioValidationResponse validate(Long scenarioId) {
        String lockKey = LOCK_PREFIX + scenarioId;
        acquireLock(lockKey);

        try {
            // 데이터 로드 '시작 시점'을 기록 (검증 중 시나리오 변경 방지)
            LocalDateTime dataLoadedAt = LocalDateTime.now();
            ScenarioValidationData data = scenarioDataReader.loadForValidation(scenarioId);

            if (!"DRAFT".equals(data.scenario().status()) && !"HIDDEN".equals(data.scenario().status())) {
                throw new AiException(AiErrorCode.SCENARIO_VALIDATION_FAILED, "DRAFT 또는 HIDDEN 상태의 시나리오만 검증할 수 있습니다.");
            }

            RuleBasedScenarioValidator.RuleValidationResult ruleResult = ruleValidator.validate(data);

            List<ValidationCheckItem> publicScoredItems = new ArrayList<>(ruleResult.publicItems());
            String problemSummary;
            String suggestion;
            boolean aiFailed = false;

            if (ruleResult.hasHardBlocker()) {
                publicScoredItems.addAll(buildZeroScoreAiItems());
                problemSummary = buildRuleOnlyProblemSummary(ruleResult.allItems());
                suggestion = "필수 항목을 먼저 보완해 주세요.";
            } else {
                AiValidationOutcome aiOutcome = callAiValidation(scenarioId, data, ruleResult.publicItems());
                aiFailed = aiOutcome.aiFailed();

                if (aiFailed) {
                    publicScoredItems.addAll(buildZeroScoreAiItems());
                } else {
                    publicScoredItems.addAll(aiOutcome.checkItems());
                }

                problemSummary = aiOutcome.problemSummary();
                suggestion = aiOutcome.suggestion();
            }

            publicScoredItems = sortPublicItems(publicScoredItems);

            int totalScore = publicScoredItems.stream().mapToInt(ValidationCheckItem::score).sum();
            int maxPossible = publicScoredItems.stream().mapToInt(ValidationCheckItem::maxScore).sum();
            int normalizedScore = maxPossible > 0 ? (totalScore * 100 / maxPossible) : 0;
            String status = determineStatus(normalizedScore, ruleResult.hasHardBlocker(), aiFailed);

            List<ValidationCheckItem> allItemsForStorage = new ArrayList<>();
            allItemsForStorage.addAll(ruleResult.allItems());
            allItemsForStorage.addAll(publicScoredItems.stream()
                    .filter(item -> item.source() == ValidationSource.AI)
                    .toList());

            ScenarioValidationResult entity = ScenarioValidationResult.builder()
                    .scenarioId(scenarioId)
                    .validationStatus(status)
                    .validationScore(normalizedScore)
                    .problemSummary(problemSummary)
                    .suggestion(suggestion)
                    .checkItemsJson(toJson(allItemsForStorage))
                    .checkedAt(dataLoadedAt)
                    .build();

            resultRepository.save(entity);

            return toResponse(scenarioId, status, normalizedScore, problemSummary, suggestion, publicScoredItems);
        } finally {
            lockService.unlock(lockKey);
        }
    }

    public ScenarioValidationResponse getLatestResult(Long scenarioId) {
        ScenarioValidationResult result = resultRepository
                .findTopByScenarioIdOrderByCheckedAtDescIdDesc(scenarioId)
                .orElseThrow(() -> new AiException(AiErrorCode.SCENARIO_VALIDATION_RESULT_NOT_FOUND));

        List<ValidationCheckItem> stored = fromJsonItems(result.getCheckItemsJson());
        List<ValidationCheckItem> publicItems = sortPublicItems(stored.stream()
                .filter(item -> item.maxScore() > 0)
                .toList());

        return toResponse(
                result.getScenarioId(),
                result.getValidationStatus(),
                result.getValidationScore(),
                result.getProblemSummary(),
                result.getSuggestion(),
                publicItems
        );
    }

    private void acquireLock(String lockKey) {
        try {
            lockService.lock(lockKey, LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
        } catch (LockException e) {
            throw new AiException(AiErrorCode.SCENARIO_VALIDATION_IN_PROGRESS, e);
        }
    }

    private AiValidationOutcome callAiValidation(
            Long scenarioId,
            ScenarioValidationData data,
            List<ValidationCheckItem> ruleItems
    ) {
        AiCallContext context = new AiCallContext(
                AiFeatureType.SCENARIO_VALIDATION,
                PROMPT_VERSION,
                scenarioId,
                null,
                null,
                null
        );

        if (aiClient.isMockMode()) {
            aiClient.recordMock(context);
            return buildMockAiOutcome();
        }

        long startTime = System.currentTimeMillis();
        try {
            String systemPrompt = "너는 추리게임 시나리오 품질 검증 AI다. JSON으로만 응답하라.";
            String userPrompt = promptBuilder.buildScenarioValidationPrompt(data, ruleItems);
            AiRequestParams params = AiRequestParams.validation(temperature, maxTokens);

            AiCallResult response = aiClient.chatWithMetadata(systemPrompt, userPrompt, params, context);
            return parseAiResponse(response.text());
        } catch (AiException e) {
            if (e.getErrorCode() instanceof AiErrorCode aiErrorCode && aiErrorCode.isRateLimitError()) {
                throw e;
            }
            log.warn("AI 검증 호출 실패: {}", e.getMessage());
            aiClient.recordFallback(context, e.getErrorCode().getCode(), elapsedMs(startTime));
            return AiValidationOutcome.failed();
        } catch (Exception e) {
            log.warn("AI 검증 호출 실패: {}", e.getMessage());
            aiClient.recordFallback(context, errorCode(e), elapsedMs(startTime));
            return AiValidationOutcome.failed();
        }
    }

    private long elapsedMs(long startTime) {
        return Math.max(0L, System.currentTimeMillis() - startTime);
    }

    private String errorCode(Exception e) {
        if (e instanceof AiException aiException) {
            return aiException.getErrorCode().getCode();
        }
        return AiErrorCode.SCENARIO_VALIDATION_FAILED.getCode();
    }

    private AiValidationOutcome parseAiResponse(String response) {
        try {
            String cleaned = stripCodeFence(response);
            AiValidationResult parsed = jsonMapper.readValue(cleaned, AiValidationResult.class);

            if (parsed.items() == null) {
                log.warn("AI 검증 응답에 items가 null");
                return AiValidationOutcome.failed();
            }

            Map<String, AiValidationResult.AiCheckItem> itemMap = parsed.items().stream()
                    .filter(item -> item.key() != null)
                    .collect(Collectors.toMap(
                            AiValidationResult.AiCheckItem::key,
                            item -> item,
                            (a, b) -> a
                    ));

            List<ValidationCheckItem> aiItems = REQUIRED_AI_KEYS.stream()
                    .map(key -> {
                        AiValidationResult.AiCheckItem item = itemMap.get(key);
                        int score = item == null ? 0 : clamp(item.score(), 0, 10);
                        return new ValidationCheckItem(
                                key,
                                AI_CHECK_NAMES.getOrDefault(key, key),
                                score >= 6,
                                score,
                                10,
                                ValidationSource.AI,
                                ValidationSeverity.WARNING
                        );
                    })
                    .toList();

            String problemSummary = StringUtils.hasText(parsed.problemSummary())
                    ? parsed.problemSummary()
                    : "AI 검증이 완료되었습니다.";
            String suggestion = StringUtils.hasText(parsed.suggestion())
                    ? parsed.suggestion()
                    : null;

            return new AiValidationOutcome(aiItems, problemSummary, suggestion, false);
        } catch (Exception e) {
            log.warn("AI 검증 응답 파싱 실패: {}", e.getMessage());
            return AiValidationOutcome.failed();
        }
    }

    private String stripCodeFence(String response) {
        if (response == null) {
            return "";
        }
        String cleaned = response.trim();
        if (!cleaned.startsWith("```")) {
            return cleaned;
        }

        int firstLineEnd = cleaned.indexOf('\n');
        if (firstLineEnd >= 0) {
            cleaned = cleaned.substring(firstLineEnd + 1).trim();
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }
        return cleaned;
    }

    private List<ValidationCheckItem> buildZeroScoreAiItems() {
        return REQUIRED_AI_KEYS.stream()
                .map(key -> new ValidationCheckItem(
                        key,
                        AI_CHECK_NAMES.getOrDefault(key, key),
                        false,
                        0,
                        10,
                        ValidationSource.AI,
                        ValidationSeverity.WARNING
                ))
                .toList();
    }

    private AiValidationOutcome buildMockAiOutcome() {
        List<ValidationCheckItem> items = List.of(
                aiItem("motive_quality", 9),
                aiItem("method_quality", 8),
                aiItem("fake_evidence_quality", 7),
                aiItem("suspect_suspicion", 7),
                aiItem("hint_strength", 8),
                aiItem("explanation_completeness", 9)
        );

        return new AiValidationOutcome(
                items,
                "전반적으로 잘 구성된 시나리오입니다. 페이크 단서를 보강하면 더 좋겠습니다.",
                "무고한 용의자에게도 숨기는 비밀을 추가하면 난이도가 올라갑니다.",
                false
        );
    }

    private ValidationCheckItem aiItem(String key, int score) {
        return new ValidationCheckItem(
                key,
                AI_CHECK_NAMES.get(key),
                score >= 6,
                score,
                10,
                ValidationSource.AI,
                ValidationSeverity.WARNING
        );
    }

    private String determineStatus(int score, boolean hasHardBlocker, boolean aiFailed) {
        if (aiFailed) {
            return ValidationStatus.FAILED.name();
        }
        if (hasHardBlocker) {
            return ValidationStatus.NEEDS_FIX.name();
        }
        if (score >= 90) {
            return ValidationStatus.PASSED.name();
        }
        if (score >= 70) {
            return ValidationStatus.PASSED_WITH_WARNINGS.name();
        }
        if (score >= 50) {
            return ValidationStatus.NEEDS_FIX.name();
        }
        return ValidationStatus.FAILED.name();
    }

    private String toJson(List<ValidationCheckItem> items) {
        try {
            return jsonMapper.writeValueAsString(items);
        } catch (Exception e) {
            log.error("시나리오 검증 항목 JSON 직렬화 실패", e);
            throw new AiException(AiErrorCode.SCENARIO_VALIDATION_FAILED, e);
        }
    }

    private List<ValidationCheckItem> fromJsonItems(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return jsonMapper.readValue(json, new TypeReference<List<ValidationCheckItem>>() {});
        } catch (Exception e) {
            log.warn("시나리오 검증 항목 JSON 역직렬화 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ValidationCheckItem> sortPublicItems(List<ValidationCheckItem> items) {
        Map<String, Integer> order = new HashMap<>();
        for (int i = 0; i < PUBLIC_CHECK_ORDER.size(); i++) {
            order.put(PUBLIC_CHECK_ORDER.get(i), i);
        }

        return items.stream()
                .sorted(Comparator.comparingInt(item ->
                        order.getOrDefault(item.key(), Integer.MAX_VALUE)))
                .toList();
    }

    private ScenarioValidationResponse toResponse(
            Long scenarioId,
            String status,
            Integer score,
            String problemSummary,
            String suggestion,
            List<ValidationCheckItem> publicItems
    ) {
        List<ScenarioValidationResponse.CheckItemDto> checkItems = sortPublicItems(publicItems).stream()
                .map(item -> new ScenarioValidationResponse.CheckItemDto(item.name(), item.passed()))
                .toList();

        return new ScenarioValidationResponse(
                scenarioId,
                status,
                score,
                problemSummary,
                suggestion,
                checkItems
        );
    }

    private String buildRuleOnlyProblemSummary(List<ValidationCheckItem> items) {
        String failedHardBlockers = items.stream()
                .filter(item -> item.severity() == ValidationSeverity.HARD)
                .filter(item -> !item.passed())
                .map(ValidationCheckItem::name)
                .collect(Collectors.joining(", "));
        if (!failedHardBlockers.isBlank()) {
            return "필수 시나리오 데이터가 부족합니다: " + failedHardBlockers;
        }
        return "필수 시나리오 데이터를 확인해 주세요.";
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
