package com.startup.domain.ai.prompt;

import com.startup.domain.ai.dto.ChatTurn;
import com.startup.domain.ai.dto.EvidenceInfo;
import com.startup.domain.ai.dto.FinalDeductionRequest;
import com.startup.domain.ai.dto.ResponsePolicyResult;
import com.startup.domain.ai.dto.ScenarioValidationData;
import com.startup.domain.ai.dto.ScoringCriteria;
import com.startup.domain.ai.dto.ScoringResult;
import com.startup.domain.ai.dto.SolutionInfo;
import com.startup.domain.ai.dto.SuspectProfile;
import com.startup.domain.ai.dto.ValidationCheckItem;
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
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AiPromptBuilder {

    private final Resource systemPromptResource;
    private final Resource userPromptResource;
    private final Resource evidenceUserPromptResource;
    private final Resource deductionScoringPromptResource;
    private final Resource scenarioValidationPromptResource;

    public AiPromptBuilder(
            @Value("classpath:prompts/interrogation_system_prompt.txt") Resource systemPromptResource,
            @Value("classpath:prompts/interrogation_user_prompt.txt") Resource userPromptResource,
            @Value("classpath:prompts/evidence_interrogation_user_prompt.txt") Resource evidenceUserPromptResource,
            @Value("classpath:prompts/final_deduction_scoring_prompt.txt") Resource deductionScoringPromptResource,
            @Value("classpath:prompts/scenario_validation_prompt.txt") Resource scenarioValidationPromptResource) {
        this.systemPromptResource = systemPromptResource;
        this.userPromptResource = userPromptResource;
        this.evidenceUserPromptResource = evidenceUserPromptResource;
        this.deductionScoringPromptResource = deductionScoringPromptResource;
        this.scenarioValidationPromptResource = scenarioValidationPromptResource;
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

    public String buildScenarioValidationPrompt(
            ScenarioValidationData data,
            List<ValidationCheckItem> ruleItems
    ) {
        String template = loadTemplate(scenarioValidationPromptResource);

        ScenarioValidationData.ScenarioBasicInfo scenario = data.scenario();
        ScenarioValidationData.SolutionValidationInfo solution = data.solution();

        return template
                .replace("{title}", nullSafe(scenario == null ? null : scenario.title()))
                .replace("{description}", nullSafe(scenario == null ? null : scenario.description()))
                .replace("{difficulty}", nullSafe(scenario == null ? null : scenario.difficulty()))
                .replace("{victimSummary}", formatVictim(data.victim()))
                .replace("{locationsSummary}", formatLocations(data.locations()))
                .replace("{suspectsSummary}", formatValidationSuspects(data.suspects(), data.suspectSecrets()))
                .replace("{evidencesSummary}", formatValidationEvidences(data.evidences()))
                .replace("{hintsSummary}", formatValidationHints(data.hints()))
                .replace("{timelineSummary}", formatTimeline(data.timelineEvents()))
                .replace("{culpritName}", nullSafe(solution == null ? null : solution.culpritName()))
                .replace("{culpritRole}", nullSafe(solution == null ? null : solution.culpritRole()))
                .replace("{motive}", nullSafe(solution == null ? null : solution.motive()))
                .replace("{method}", nullSafe(solution == null ? null : solution.method()))
                .replace("{coverUp}", nullSafe(solution == null ? null : solution.coverUp()))
                .replace("{fullExplanation}", nullSafe(solution == null ? null : solution.fullExplanation()))
                .replace("{solutionEvidenceSummary}", formatSolutionEvidences(data.solutionEvidences()))
                .replace("{ruleCheckSummary}", formatValidationRuleChecks(ruleItems));
    }

    private String buildFreeUserPrompt(SuspectProfile suspect,
                                       List<EvidenceInfo> revealedEvidences,
                                       EvidenceInfo presentedEvidence,
                                       ResponsePolicyResult policy,
                                       List<ChatTurn> history,
                                       String question) {
        String template = loadTemplate(userPromptResource);
        return template
                .replace("{allowedFacts}", formatFacts(policy.allowedFacts()))
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
                .replace("{allowedFacts}", formatFacts(policy.allowedFacts()))
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

    private String formatFacts(List<String> facts) {
        if (facts == null || facts.isEmpty()) {
            return "없음";
        }
        List<String> lines = facts.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(fact -> !fact.isBlank())
                .map(this::sanitizeFact)
                .toList();
        if (lines.isEmpty()) {
            return "없음";
        }
        return lines.stream()
                .map(fact -> "- " + fact)
                .collect(Collectors.joining("\n"));
    }

    private String sanitizeFact(String fact) {
        return fact
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace("{", "｛")
                .replace("}", "｝")
                .strip();
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

    private String formatVictim(ScenarioValidationData.VictimInfo victim) {
        if (victim == null) {
            return "없음";
        }
        return "이름: " + nullSafe(victim.name()) + "\n"
                + "역할: " + nullSafe(victim.role()) + "\n"
                + "사망 원인: " + nullSafe(victim.causeOfDeath()) + "\n"
                + "발견 상태: " + nullSafe(victim.foundCondition());
    }

    private String formatLocations(List<ScenarioValidationData.LocationInfo> locations) {
        if (locations == null || locations.isEmpty()) {
            return "없음";
        }
        return locations.stream()
                .map(location -> "- " + nullSafe(location.name()) + ": " + nullSafe(location.description()))
                .collect(Collectors.joining("\n"));
    }

    private String formatValidationSuspects(List<ScenarioValidationData.SuspectValidationInfo> suspects,
                                            List<ScenarioValidationData.SuspectSecretInfo> secrets) {
        if (suspects == null || suspects.isEmpty()) {
            return "없음";
        }
        return suspects.stream()
                .map(suspect -> "- " + nullSafe(suspect.name()) + " (" + nullSafe(suspect.role()) + ")"
                        + "\n  공개 프로필: " + nullSafe(suspect.publicProfile())
                        + "\n  공개 진술: " + nullSafe(suspect.publicStatement())
                        + "\n  알리바이: " + nullSafe(suspect.alibi())
                        + "\n  범인 여부: " + suspect.isCulprit()
                        + "\n  비밀: " + formatSecretsForSuspect(suspect.suspectId(), secrets))
                .collect(Collectors.joining("\n"));
    }

    private String formatSecretsForSuspect(Long suspectId, List<ScenarioValidationData.SuspectSecretInfo> secrets) {
        if (secrets == null || secrets.isEmpty()) {
            return "없음";
        }
        String summary = secrets.stream()
                .filter(secret -> Objects.equals(secret.suspectId(), suspectId))
                .map(secret -> nullSafe(secret.title()) + ": " + nullSafe(secret.content()))
                .collect(Collectors.joining(" / "));
        return summary.isBlank() ? "없음" : summary;
    }

    private String formatValidationEvidences(List<ScenarioValidationData.EvidenceValidationInfo> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return "없음";
        }
        return evidences.stream()
                .map(evidence -> "- [" + evidence.evidenceId() + "] " + nullSafe(evidence.title())
                        + ": " + nullSafe(evidence.description())
                        + " / 중요도=" + nullSafe(evidence.importance())
                        + " / 초기공개=" + evidence.isInitialPublic())
                .collect(Collectors.joining("\n"));
    }

    private String formatValidationHints(List<ScenarioValidationData.HintValidationInfo> hints) {
        if (hints == null || hints.isEmpty()) {
            return "없음";
        }
        return hints.stream()
                .map(hint -> "- Lv." + hint.hintLevel() + ": " + nullSafe(hint.content())
                        + " / penalty=" + hint.penaltyScore())
                .collect(Collectors.joining("\n"));
    }

    private String formatTimeline(List<ScenarioValidationData.TimelineEventInfo> events) {
        if (events == null || events.isEmpty()) {
            return "없음";
        }
        return events.stream()
                .map(event -> "- " + nullSafe(event.eventTime()) + " " + nullSafe(event.title())
                        + ": " + nullSafe(event.description())
                        + " / type=" + nullSafe(event.eventType())
                        + " / true=" + event.isTrueEvent())
                .collect(Collectors.joining("\n"));
    }

    private String formatSolutionEvidences(List<ScenarioValidationData.SolutionEvidenceInfo> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return "없음";
        }
        return evidences.stream()
                .map(evidence -> "- [" + evidence.evidenceId() + "] " + nullSafe(evidence.reason()))
                .collect(Collectors.joining("\n"));
    }

    private String formatValidationRuleChecks(List<ValidationCheckItem> ruleItems) {
        if (ruleItems == null || ruleItems.isEmpty()) {
            return "없음";
        }
        return ruleItems.stream()
                .map(item -> "- " + item.name() + ": " + (item.passed() ? "통과" : "실패"))
                .collect(Collectors.joining("\n"));
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
