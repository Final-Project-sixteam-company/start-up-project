package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.ScenarioValidationData;
import com.startup.domain.ai.dto.ValidationCheckItem;
import com.startup.domain.ai.enums.ValidationSeverity;
import com.startup.domain.ai.enums.ValidationSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class RuleBasedScenarioValidator {

    public RuleValidationResult validate(ScenarioValidationData data) {
        List<ValidationCheckItem> internalBlockers = new ArrayList<>();
        List<ValidationCheckItem> publicItems = new ArrayList<>();
        boolean hasHardBlocker = false;

        boolean hasSolution = data.solution() != null
                && data.solution().culpritSuspectId() != null;
        internalBlockers.add(hardBlocker("has_solution", "Solution 존재", hasSolution));
        if (!hasSolution) {
            hasHardBlocker = true;
        }

        boolean hasSuspects = data.suspects() != null && data.suspects().size() >= 2;
        internalBlockers.add(hardBlocker("has_suspects", "용의자 2명 이상", hasSuspects));
        if (!hasSuspects) {
            hasHardBlocker = true;
        }

        boolean hasEvidences = data.evidences() != null && data.evidences().size() >= 3;
        internalBlockers.add(hardBlocker("has_evidences", "증거 3개 이상", hasEvidences));
        if (!hasEvidences) {
            hasHardBlocker = true;
        }

        boolean hasHints = data.hints() != null && !data.hints().isEmpty();
        internalBlockers.add(hardBlocker("has_hints", "힌트 1개 이상", hasHints));
        if (!hasHints) {
            hasHardBlocker = true;
        }

        boolean hasKeyEvidenceIds = data.solution() != null
                && data.solution().keyEvidenceIds() != null
                && !data.solution().keyEvidenceIds().isEmpty();
        boolean hasSolutionEvidences = data.solutionEvidences() != null
                && !data.solutionEvidences().isEmpty();
        boolean hasKeyEvidence = hasKeyEvidenceIds || hasSolutionEvidences;
        internalBlockers.add(hardBlocker("has_key_evidence", "핵심 증거 연결 존재", hasKeyEvidence));
        if (!hasKeyEvidence) {
            hasHardBlocker = true;
        }

        publicItems.add(scoredRule("culprit_set", "범인 설정 여부", hasSolution));
        publicItems.add(scoredRule("key_evidence_exist", "결정적 증거 존재 여부", hasKeyEvidence));

        boolean allAlibi = data.suspects() != null && !data.suspects().isEmpty()
                && data.suspects().stream()
                .allMatch(suspect -> suspect.alibi() != null && !suspect.alibi().isBlank());
        publicItems.add(scoredRule("all_alibi", "각 용의자 알리바이 존재", allAlibi));

        boolean allPolicy = data.suspects() != null && !data.suspects().isEmpty()
                && data.responsePolicies() != null
                && data.suspects().stream().allMatch(suspect ->
                data.responsePolicies().stream()
                        .anyMatch(policy -> Objects.equals(policy.suspectId(), suspect.suspectId())));
        publicItems.add(scoredRule("all_response_policy", "AI NPC 답변 정책 정의 여부", allPolicy));

        List<ValidationCheckItem> allItems = new ArrayList<>();
        allItems.addAll(internalBlockers);
        allItems.addAll(publicItems);

        return new RuleValidationResult(allItems, publicItems, hasHardBlocker);
    }

    private ValidationCheckItem hardBlocker(String key, String name, boolean passed) {
        return new ValidationCheckItem(
                key,
                name,
                passed,
                0,
                0,
                ValidationSource.RULE,
                ValidationSeverity.HARD
        );
    }

    private ValidationCheckItem scoredRule(String key, String name, boolean passed) {
        return new ValidationCheckItem(
                key,
                name,
                passed,
                passed ? 10 : 0,
                10,
                ValidationSource.RULE,
                ValidationSeverity.WARNING
        );
    }

    public record RuleValidationResult(
            List<ValidationCheckItem> allItems,
            List<ValidationCheckItem> publicItems,
            boolean hasHardBlocker
    ) {
    }
}
