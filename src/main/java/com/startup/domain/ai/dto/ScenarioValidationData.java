package com.startup.domain.ai.dto;

import java.util.List;

public record ScenarioValidationData(
        ScenarioBasicInfo scenario,
        VictimInfo victim,
        List<LocationInfo> locations,
        List<SuspectValidationInfo> suspects,
        List<EvidenceValidationInfo> evidences,
        List<HintValidationInfo> hints,
        List<TimelineEventInfo> timelineEvents,
        SolutionValidationInfo solution,
        List<SolutionEvidenceInfo> solutionEvidences,
        List<ResponsePolicyInfo> responsePolicies,
        List<SuspectSecretInfo> suspectSecrets
) {
    public record ScenarioBasicInfo(
            Long scenarioId,
            String title,
            String description,
            String difficulty,
            String status
    ) {
    }

    public record VictimInfo(
            String name,
            String role,
            String causeOfDeath,
            String foundCondition
    ) {
    }

    public record LocationInfo(
            Long locationId,
            String name,
            String description
    ) {
    }

    public record SuspectValidationInfo(
            Long suspectId,
            String name,
            String role,
            String alibi,
            String publicStatement,
            String publicProfile,
            boolean isCulprit
    ) {
    }

    public record EvidenceValidationInfo(
            Long evidenceId,
            String title,
            String description,
            String importance,
            boolean isInitialPublic,
            List<Long> relatedSuspectIds
    ) {
    }

    public record HintValidationInfo(
            Long hintId,
            int hintLevel,
            String content,
            int penaltyScore
    ) {
    }

    public record TimelineEventInfo(
            String eventTime,
            String title,
            String description,
            String eventType,
            boolean isTrueEvent
    ) {
    }

    public record SolutionValidationInfo(
            Long culpritSuspectId,
            String culpritName,
            String culpritRole,
            String motive,
            String method,
            String coverUp,
            String fullExplanation,
            List<Long> keyEvidenceIds
    ) {
    }

    public record SolutionEvidenceInfo(
            Long evidenceId,
            String reason
    ) {
    }

    public record ResponsePolicyInfo(
            Long suspectId,
            String conditionKey,
            String policyText,
            int priority
    ) {
    }

    public record SuspectSecretInfo(
            Long suspectId,
            String title,
            String content,
            boolean isCoreSecret
    ) {
    }
}
