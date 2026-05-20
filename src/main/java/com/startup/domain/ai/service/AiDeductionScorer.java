package com.startup.domain.ai.service;

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
import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import com.startup.domain.ai.prompt.AiPromptBuilder;
import com.startup.domain.ai.repository.FinalDeductionEvidenceRepository;
import com.startup.domain.ai.support.DeductionContextLoader;
import com.startup.domain.ai.support.FallbackFeedbackGenerator;
import com.startup.domain.ai.support.HintPenaltyReader;
import com.startup.domain.ai.support.PlaySessionReader;
import com.startup.domain.ai.support.RuleBasedScorer;
import com.startup.domain.ai.support.ScoringCriteriaProvider;
import com.startup.domain.ai.support.SolutionReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiDeductionScorer {

    private final DeductionContextLoader contextLoader;
    private final PlaySessionReader playSessionReader;
    private final SolutionReader solutionReader;
    private final HintPenaltyReader hintPenaltyReader;
    private final ScoringCriteriaProvider scoringCriteriaProvider;
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
            // 1. 중복 제출 확인 + 잠금 (트랜잭션 1) — 세션을 COMPLETED로 변경하지 않음
            contextLoader.ensureNotSubmitted(sessionId);
            locked = true;

            // 2. 채점 수행 (트랜잭션 밖)
            Long scenarioId = playSessionReader.getScenarioId(sessionId);
            SolutionInfo solution = solutionReader.findByScenarioId(scenarioId);
            ScoringCriteria criteria = scoringCriteriaProvider.getByCriteria(scenarioId);

            ScoringResult scoringResult = ruleBasedScorer.score(request, criteria);

            int hintPenalty = hintPenaltyReader.getTotalPenalty(sessionId);
            int finalScore = Math.max(0, scoringResult.totalScore() - hintPenalty);
            String grade = calculateGrade(finalScore);

            // 3. AI 피드백 (트랜잭션 밖)
            AiFeedbackResult feedbackResult = generateFeedback(scoringResult, solution, request, criteria);

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
        } catch (AiException e) {
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

    public DeductionResultResponse getResult(Long sessionId) {
        FinalDeduction deduction = contextLoader.findBySessionId(sessionId);
        if (deduction == null) {
            throw new AiException(AiErrorCode.DEDUCTION_RESULT_NOT_FOUND);
        }

        Long scenarioId = playSessionReader.getScenarioId(sessionId);
        SolutionInfo solution = solutionReader.findByScenarioId(scenarioId);
        ScoringCriteria criteria = scoringCriteriaProvider.getByCriteria(scenarioId);

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
                criteria.keyEvidenceIds().stream()
                        .map(id -> new DeductionResultResponse.EvidenceDto(
                                id, solution.evidenceTitles().getOrDefault(id, null)))
                        .toList(),
                List.of()
        );
    }

    private AiFeedbackResult generateFeedback(ScoringResult scoringResult,
                                               SolutionInfo solution,
                                               FinalDeductionRequest request,
                                               ScoringCriteria criteria) {
        if (aiClient.isMockMode()) {
            return fallbackFeedbackGenerator.generate(scoringResult, criteria);
        }

        try {
            String systemPrompt = "너는 추리게임 채점 보조 AI다. JSON으로만 응답하라.";
            String userPrompt = promptBuilder.buildDeductionScoringPrompt(
                    scoringResult, solution, request, criteria);
            AiRequestParams params = AiRequestParams.deduction(temperature, maxTokens);

            String response = aiClient.chat(systemPrompt, userPrompt, params);
            return parseAiFeedback(response);
        } catch (Exception e) {
            log.warn("AI 피드백 생성 실패, Fallback 사용: {}", e.getMessage());
            return fallbackFeedbackGenerator.generate(scoringResult, criteria);
        }
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
}
