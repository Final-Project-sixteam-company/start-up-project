package com.startup.domain.ai.dto;

import java.util.List;

public record ScoringCriteria(
        Long scenarioId,
        Long culpritSuspectId,
        KeywordCriteria method,
        KeywordCriteria motive,
        KeywordCriteria coverUp,
        List<Long> keyEvidenceIds,
        int evidenceMaxScore,
        int culpritMaxScore
) {
    public record KeywordCriteria(List<String> keywords, int minMatch, int maxScore) {}
}
