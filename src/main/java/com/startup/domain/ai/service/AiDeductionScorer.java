package com.startup.domain.ai.service;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.error.BusinessException;
import com.startup.common.error.CommonErrorCode;
import com.startup.domain.ai.client.AiCallContext;
import com.startup.domain.ai.client.AiCallResult;
import com.startup.domain.ai.client.AiClient;
import com.startup.domain.ai.client.AiRequestParams;
import com.startup.domain.ai.dto.AiFeedbackResult;
import com.startup.domain.ai.dto.DeductionResultResponse;
import com.startup.domain.ai.dto.FinalDeductionRequest;
import com.startup.domain.ai.dto.FinalDeductionResponse;
import com.startup.domain.ai.dto.ScoringCriteria;
import com.startup.domain.ai.dto.ScoringResult;
import com.startup.domain.ai.dto.SolutionInfo;
import com.startup.domain.ai.entity.FinalDeduction;
import com.startup.domain.ai.entity.FinalDeductionEvidence;
import com.startup.domain.ai.enums.AiFeatureType;
import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import com.startup.domain.ai.prompt.AiPromptBuilder;
import com.startup.domain.ai.repository.FinalDeductionEvidenceRepository;
import com.startup.domain.ai.support.DeductionScoringDefaults;
import com.startup.domain.ai.support.DeductionContextLoader;
import com.startup.domain.ai.support.EvidenceReader;
import com.startup.domain.ai.support.FallbackFeedbackGenerator;
import com.startup.domain.ai.support.HintPenaltyReader;
import com.startup.domain.ai.support.PlaySessionReader;
import com.startup.domain.ai.support.RuleBasedScorer;
import com.startup.domain.ai.support.SolutionReader;
import com.startup.domain.ai.support.SuspectReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiDeductionScorer {

    private static final String PROMPT_VERSION = "final_deduction_scoring_v1";

    private final MockUserProvider mockUserProvider;
    private final DeductionContextLoader contextLoader;
    private final PlaySessionReader playSessionReader;
    private final EvidenceReader evidenceReader;
    private final SuspectReader suspectReader;
    private final SolutionReader solutionReader;
    private final HintPenaltyReader hintPenaltyReader;
    private final DeductionScoringDefaults scoringDefaults;
    private final RuleBasedScorer ruleBasedScorer;
    private final FallbackFeedbackGenerator fallbackFeedbackGenerator;
    private final AiPromptBuilder promptBuilder;
    private final AiClient aiClient;
    private final JsonMapper jsonMapper;
    private final FinalDeductionEvidenceRepository finalDeductionEvidenceRepository;

    @Value("${caselab.ai.deduction.temperature:0.2}")
    private double temperature;

    @Value("${caselab.ai.deduction.max-tokens:1500}")
    private int maxTokens;

    public FinalDeductionResponse submitAndScore(Long sessionId, FinalDeductionRequest request) {
        boolean locked = false;
        try {
            //세션 소유자 검증
            Long currentUserId = mockUserProvider.currentUserId();
            Long ownerUserId = playSessionReader.getOwnerUserId(sessionId);
            if (!Objects.equals(currentUserId, ownerUserId)) {
                throw new BusinessException(CommonErrorCode.ACCESS_DENIED);
            }

            // 1. 중복 제출 확인 + 잠금 (트랜잭션 1) — 세션을 COMPLETED로 변경하지 않음
            contextLoader.ensureNotSubmitted(sessionId);
            locked = true;

            // 2. 채점 수행 (트랜잭션 밖)
            Long scenarioId = playSessionReader.getScenarioId(sessionId);
            evidenceReader.syncTimeUnlocks(sessionId, currentUserId);
            suspectReader.findByIdAndScenarioId(request.selectedCulpritId(), scenarioId);
            validateSelectedEvidenceUnlocked(sessionId, request.selectedEvidenceIds());

            Long variantId = playSessionReader.getScenarioVariantId(sessionId);
            SolutionInfo solution = solutionReader.findByScenarioIdAndVariantId(scenarioId, variantId);
            ScoringCriteria criteria = buildCriteriaFromSolution(solution, scenarioId);

            ScoringResult scoringResult = ruleBasedScorer.score(request, criteria);

            int hintPenalty = hintPenaltyReader.getTotalPenalty(sessionId);
            int finalScore = Math.max(0, scoringResult.totalScore() - hintPenalty);
            String grade = calculateGrade(finalScore);

            // 3. AI 피드백 (트랜잭션 밖)
            AiFeedbackResult feedbackResult = generateFeedback(sessionId, scoringResult, solution, request, criteria);

            // 4. 결과 저장 + 세션 완료 (트랜잭션 2) — 저장 성공 시에만 COMPLETED
            LocalDateTime submittedAt = LocalDateTime.now();

            FinalDeduction entity = FinalDeduction.builder()
                    .playSessionId(sessionId)
                    .selectedCulpritId(request.selectedCulpritId())
                    .motiveText(request.motiveText())
                    .methodText(request.methodText())
                    .coverUpText(request.coverUpText())
                    .score(finalScore)
                    .grade(grade)
                    .feedback(feedbackResult.feedback())
                    .matchedParts(toJson(feedbackResult.matchedParts()))
                    .missedParts(toJson(feedbackResult.missedParts()))
                    .submittedAt(submittedAt)
                    .build();

            List<Long> distinctEvidenceIds = request.selectedEvidenceIds().stream()
                    .distinct()
                    .toList();

            FinalDeduction saved = contextLoader.saveResultAndComplete(sessionId, entity, distinctEvidenceIds);
            locked = false;

            return new FinalDeductionResponse(
                    saved.getId(),
                    finalScore,
                    grade,
                    feedbackResult.feedback(),
                    true,
                    submittedAt
            );
        } catch (BusinessException e) {
            if (locked) {
                releaseLockQuietly(sessionId);
            }
            throw e;
        } catch (DataIntegrityViolationException e) {
            if (locked) {
                releaseLockQuietly(sessionId);
            }
            log.warn("최종 추리 중복 제출 감지. sessionId={}", sessionId, e);
            throw new AiException(AiErrorCode.FINAL_DEDUCTION_ALREADY_SUBMITTED);
        } catch (Exception e) {
            if (locked) {
                releaseLockQuietly(sessionId);
            }
            log.error("최종 추리 채점 처리 실패. sessionId={}", sessionId, e);
            throw new AiException(AiErrorCode.SCORING_FAILED);
        }
    }

    private void validateSelectedEvidenceUnlocked(Long sessionId, List<Long> selectedEvidenceIds) {
        Set<Long> unlockedEvidenceIds = new HashSet<>(evidenceReader.getUnlockedEvidenceIds(sessionId));
        boolean allUnlocked = selectedEvidenceIds.stream().allMatch(unlockedEvidenceIds::contains);
        if (!allUnlocked) {
            throw new AiException(AiErrorCode.FINAL_DEDUCTION_EVIDENCE_NOT_UNLOCKED);
        }
    }

    public DeductionResultResponse getResult(Long sessionId) {
        validateResultOwner(sessionId);

        FinalDeduction deduction = contextLoader.findBySessionId(sessionId);
        if (deduction == null) {
            throw new AiException(AiErrorCode.DEDUCTION_RESULT_NOT_FOUND);
        }

        Long scenarioId = playSessionReader.getScenarioId(sessionId);
        Long variantId = playSessionReader.getScenarioVariantId(sessionId);
        SolutionInfo solution = solutionReader.findByScenarioIdAndVariantId(scenarioId, variantId);
        ScoringCriteria criteria = buildCriteriaFromSolution(solution, scenarioId);
        warnIfKeyEvidenceSourcesDiverge(criteria, solution);

        List<FinalDeductionEvidence> evidences =
                finalDeductionEvidenceRepository.findAllByFinalDeductionId(deduction.getId());

        List<Long> evidenceIds = evidences.stream()
                .map(FinalDeductionEvidence::getEvidenceId)
                .toList();

        ScoringResult scoring = ruleBasedScorer.score(
                new FinalDeductionRequest(
                        deduction.getSelectedCulpritId(),
                        deduction.getMotiveText(),
                        deduction.getMethodText(),
                        deduction.getCoverUpText(),
                        evidenceIds
                ),
                criteria
        );

        List<String> matchedParts = fromJson(deduction.getMatchedParts());
        List<String> missedParts = fromJson(deduction.getMissedParts());

        return new DeductionResultResponse(
                sessionId,
                deduction.getScore(),
                deduction.getGrade(),
                new DeductionResultResponse.CorrectCulpritDto(
                        solution.culpritSuspectId(),
                        solution.culpritName(),
                        solution.culpritRole()),
                new DeductionResultResponse.MatchedDto(
                        scoring.culpritCorrect(),
                        scoring.motiveScore() == criteria.motive().maxScore(),
                        scoring.methodScore() == criteria.method().maxScore(),
                        scoring.coverUpScore() == criteria.coverUp().maxScore(),
                        scoring.evidenceMatchCount()
                ),
                matchedParts,
                missedParts,
                deduction.getFeedback(),
                solution.fullExplanation(),
                buildKeyEvidenceDtos(solution),
                List.of()
        );
    }

    private void validateResultOwner(Long sessionId) {
        Long currentUserId = mockUserProvider.currentUserId();
        Long ownerUserId = playSessionReader.getOwnerUserId(sessionId);
        if (!Objects.equals(currentUserId, ownerUserId)) {
            throw new BusinessException(CommonErrorCode.ACCESS_DENIED, "Deduction result access is denied.");
        }
    }

    private List<DeductionResultResponse.EvidenceDto> buildKeyEvidenceDtos(SolutionInfo solution) {
        return solution.keyEvidenceIds().stream()
                .map(id -> new DeductionResultResponse.EvidenceDto(id, solution.evidenceTitles().get(id)))
                .toList();
    }

    private void warnIfKeyEvidenceSourcesDiverge(ScoringCriteria criteria, SolutionInfo solution) {
        if (!new LinkedHashSet<>(criteria.keyEvidenceIds()).equals(new LinkedHashSet<>(solution.keyEvidenceIds()))) {
            log.warn("keyEvidenceIds 소스 불일치. criteria={}, solution={}",
                    criteria.keyEvidenceIds(), solution.keyEvidenceIds());
        }
    }

    private AiFeedbackResult generateFeedback(Long sessionId,
                                              ScoringResult scoringResult,
                                              SolutionInfo solution,
                                              FinalDeductionRequest request,
                                              ScoringCriteria criteria) {
        AiCallContext context = new AiCallContext(
                AiFeatureType.FINAL_DEDUCTION,
                PROMPT_VERSION,
                criteria.scenarioId(),
                sessionId,
                null,
                null
        );

        if (aiClient.isMockMode()) {
            aiClient.recordMock(context);
            return fallbackFeedbackGenerator.generate(scoringResult, criteria);
        }

        try {
            String systemPrompt = "너는 추리게임 채점 보조 AI다. JSON으로만 응답하라.";
            String userPrompt = promptBuilder.buildDeductionScoringPrompt(
                    scoringResult, solution, request, criteria);
            AiRequestParams params = AiRequestParams.deduction(temperature, maxTokens);

            AiCallResult response = aiClient.chatWithMetadata(systemPrompt, userPrompt, params, context);
            return parseAiFeedback(response.text());
        } catch (Exception e) {
            log.warn("AI 피드백 생성 실패, Fallback 사용: {}", e.getMessage());
            aiClient.recordFallback(context, errorCode(e));
            return fallbackFeedbackGenerator.generate(scoringResult, criteria);
        }
    }

    private String errorCode(Exception e) {
        if (e instanceof AiException aiException) {
            return aiException.getErrorCode().getCode();
        }
        return AiErrorCode.SCORING_FAILED.getCode();
    }

    private AiFeedbackResult parseAiFeedback(String response) {
        try {
            return jsonMapper.readValue(response, AiFeedbackResult.class);
        } catch (Exception e) {
            log.warn("AI 피드백 JSON 파싱 실패: {}", e.getMessage());
            throw new AiException(AiErrorCode.AI_RESPONSE_PARSE_ERROR, e);
        }
    }

    private String calculateGrade(int score) {
        if (score >= 90) return "S";
        if (score >= 80) return "A";
        if (score >= 70) return "B";
        if (score >= 60) return "C";
        return "D";
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return jsonMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return jsonMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private void releaseLockQuietly(Long sessionId) {
        try {
            contextLoader.releaseFinalDeductionLock(sessionId);
        } catch (Exception releaseException) {
            log.warn("최종 추리 in-flight lock 해제 실패. sessionId={}", sessionId, releaseException);
        }
    }

    /**
     * DB의 SolutionInfo를 기준으로 채점 기준을 조립한다.
     * 항목별 배점은 기본 배점 spec을 사용하고 proofDimensionJson은 후속 채점 트랙으로 분리한다.
     */
    private ScoringCriteria buildCriteriaFromSolution(SolutionInfo solution, Long scenarioId) {
        validateSolutionForScoring(solution);
        return new ScoringCriteria(
                scenarioId,
                solution.culpritSuspectId(),
                extractKeywords(solution.method(), scoringDefaults.methodMaxScore()),
                extractKeywords(solution.motive(), scoringDefaults.motiveMaxScore()),
                extractKeywords(solution.coverUp(), scoringDefaults.coverUpMaxScore()),
                solution.keyEvidenceIds(),
                scoringDefaults.evidenceMaxScore(),
                scoringDefaults.culpritMaxScore()
        );
    }

    private void validateSolutionForScoring(SolutionInfo solution) {
        if (solution == null
                || solution.culpritSuspectId() == null
                || solution.keyEvidenceIds() == null
                || solution.keyEvidenceIds().isEmpty()
                || solution.keyEvidenceIds().stream().anyMatch(Objects::isNull)
                || isBlank(solution.method())
                || isBlank(solution.motive())
                || isBlank(solution.coverUp())) {
            throw new AiException(AiErrorCode.SOLUTION_NOT_FOUND);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ScoringCriteria.KeywordCriteria extractKeywords(String text, int maxScore) {
        List<String> words = java.util.Arrays.stream(text.split("[\\s\\p{Punct}]+"))
                .filter(w -> w.length() >= 2)
                .toList();

        if (words.isEmpty()) {
            throw new AiException(AiErrorCode.SOLUTION_NOT_FOUND);
        }

        int minMatch = Math.max(1, words.size() / 2);
        return new ScoringCriteria.KeywordCriteria(words, minMatch, maxScore);
    }
}
