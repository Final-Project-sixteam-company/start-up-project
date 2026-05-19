package com.startup.domain.ai.prompt;

import com.startup.domain.ai.dto.ChatTurn;
import com.startup.domain.ai.dto.EvidenceInfo;
import com.startup.domain.ai.dto.FinalDeductionRequest;
import com.startup.domain.ai.dto.ResponsePolicyResult;
import com.startup.domain.ai.dto.ScoringCriteria;
import com.startup.domain.ai.dto.ScoringResult;
import com.startup.domain.ai.dto.SolutionInfo;
import com.startup.domain.ai.dto.SuspectProfile;
import com.startup.domain.ai.enums.QuestionType;
import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AiPromptBuilder {

    private final Resource systemPromptResource;
    private final Resource userPromptResource;
    private final Resource evidenceUserPromptResource;
    private final Resource deductionScoringPromptResource;

    public AiPromptBuilder(
            @Value("classpath:prompts/interrogation_system_prompt.txt") Resource systemPromptResource,
            @Value("classpath:prompts/interrogation_user_prompt.txt") Resource userPromptResource,
            @Value("classpath:prompts/evidence_interrogation_user_prompt.txt") Resource evidenceUserPromptResource,
            @Value("classpath:prompts/final_deduction_scoring_prompt.txt") Resource deductionScoringPromptResource) {
        this.systemPromptResource = systemPromptResource;
        this.userPromptResource = userPromptResource;
        this.evidenceUserPromptResource = evidenceUserPromptResource;
        this.deductionScoringPromptResource = deductionScoringPromptResource;
    }

    public String buildSystemPrompt() {
        return loadTemplate(systemPromptResource);
    }

    public String buildUserPrompt(SuspectProfile suspect,
                                  List<EvidenceInfo> revealedEvidences,
                                  EvidenceInfo presentedEvidence,
                                  ResponsePolicyResult policy,
                                  List<ChatTurn> history,
                                  String question,
                                  QuestionType questionType) {
        if (questionType == QuestionType.EVIDENCE_PRESENTED && presentedEvidence != null) {
            return buildEvidenceUserPrompt(suspect, presentedEvidence, revealedEvidences, policy, history, question);
        }
        return buildFreeUserPrompt(suspect, revealedEvidences, presentedEvidence, policy, history, question);
    }

    public String buildDeductionScoringPrompt(ScoringResult result,
                                               SolutionInfo solution,
                                               FinalDeductionRequest request,
                                               ScoringCriteria criteria) {
        String template = loadTemplate(deductionScoringPromptResource);
        return template
                .replace("{totalScore}", String.valueOf(result.totalScore()))
                .replace("{culpritScore}", String.valueOf(result.culpritScore()))
                .replace("{culpritMaxScore}", String.valueOf(criteria.culpritMaxScore()))
                .replace("{culpritResult}", result.culpritCorrect() ? "정답" : "오답")
                .replace("{methodScore}", String.valueOf(result.methodScore()))
                .replace("{methodMaxScore}", String.valueOf(criteria.method().maxScore()))
                .replace("{methodResult}", result.methodScore() == criteria.method().maxScore() ? "정답" : (result.methodScore() > 0 ? "부분 정답" : "오답"))
                .replace("{motiveScore}", String.valueOf(result.motiveScore()))
                .replace("{motiveMaxScore}", String.valueOf(criteria.motive().maxScore()))
                .replace("{motiveResult}", result.motiveScore() == criteria.motive().maxScore() ? "정답" : (result.motiveScore() > 0 ? "부분 정답" : "오답"))
                .replace("{coverUpScore}", String.valueOf(result.coverUpScore()))
                .replace("{coverUpMaxScore}", String.valueOf(criteria.coverUp().maxScore()))
                .replace("{coverUpResult}", result.coverUpScore() == criteria.coverUp().maxScore() ? "정답" : (result.coverUpScore() > 0 ? "부분 정답" : "오답"))
                .replace("{evidenceScore}", String.valueOf(result.evidenceScore()))
                .replace("{evidenceMaxScore}", String.valueOf(criteria.evidenceMaxScore()))
                .replace("{evidenceMatchCount}", String.valueOf(result.evidenceMatchCount()))
                .replace("{correctCulprit}", nullSafe(solution.culpritName() != null
                        ? solution.culpritName() + " (" + solution.culpritRole() + ")"
                        : solution.culpritSuspectId() + "번 용의자"))
                .replace("{correctMotive}", nullSafe(solution.motive()))
                .replace("{correctMethod}", nullSafe(solution.method()))
                .replace("{correctCoverUp}", nullSafe(solution.coverUp()))
                .replace("{userCulprit}", request.selectedCulpritId() + "번 용의자")
                .replace("{userMotive}", nullSafe(request.motiveText()))
                .replace("{userMethod}", nullSafe(request.methodText()))
                .replace("{userCoverUp}", nullSafe(request.coverUpText()));
    }

    private String buildFreeUserPrompt(SuspectProfile suspect,
                                       List<EvidenceInfo> revealedEvidences,
                                       EvidenceInfo presentedEvidence,
                                       ResponsePolicyResult policy,
                                       List<ChatTurn> history,
                                       String question) {
        String template = loadTemplate(userPromptResource);
        return template
                .replace("{suspectName}", nullSafe(suspect.name()))
                .replace("{suspectRole}", nullSafe(suspect.role()))
                .replace("{relationToVictim}", nullSafe(suspect.relationToVictim()))
                .replace("{publicProfile}", nullSafe(suspect.publicProfile()))
                .replace("{publicStatement}", nullSafe(suspect.publicStatement()))
                .replace("{publicAlibi}", nullSafe(suspect.alibi()))
                .replace("{revealedEvidenceSummary}", formatEvidences(revealedEvidences))
                .replace("{presentedEvidenceSummary}", formatPresentedEvidence(presentedEvidence))
                .replace("{history}", formatHistory(history))
                .replace("{responsePolicy}", nullSafe(policy.policyText()))
                .replace("{tone}", nullSafe(policy.tone()))
                .replace("{question}", nullSafe(question));
    }

    private String buildEvidenceUserPrompt(SuspectProfile suspect,
                                           EvidenceInfo presentedEvidence,
                                           List<EvidenceInfo> revealedEvidences,
                                           ResponsePolicyResult policy,
                                           List<ChatTurn> history,
                                           String question) {
        String template = loadTemplate(evidenceUserPromptResource);
        return template
                .replace("{suspectName}", nullSafe(suspect.name()))
                .replace("{suspectRole}", nullSafe(suspect.role()))
                .replace("{publicAlibi}", nullSafe(suspect.alibi()))
                .replace("{evidenceTitle}", nullSafe(presentedEvidence.title()))
                .replace("{evidenceDescription}", nullSafe(presentedEvidence.description()))
                .replace("{gameStateSummary}", formatEvidences(revealedEvidences))
                .replace("{history}", formatHistory(history))
                .replace("{responsePolicy}", nullSafe(policy.policyText()))
                .replace("{question}", nullSafe(question));
    }

    private String formatEvidences(List<EvidenceInfo> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return "없음";
        }
        return evidences.stream()
                .map(e -> "- " + e.title() + ": " + e.description())
                .collect(Collectors.joining("\n"));
    }

    private String formatPresentedEvidence(EvidenceInfo evidence) {
        if (evidence == null) {
            return "없음";
        }
        return evidence.title() + ": " + evidence.description();
    }

    private String formatHistory(List<ChatTurn> history) {
        if (history == null || history.isEmpty()) {
            return "없음";
        }
        return history.stream()
                .map(turn -> "사용자: " + turn.question() + "\n용의자: " + turn.answer())
                .collect(Collectors.joining("\n\n"));
    }

    private String loadTemplate(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AiException(AiErrorCode.PROMPT_BUILD_ERROR, e);
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
