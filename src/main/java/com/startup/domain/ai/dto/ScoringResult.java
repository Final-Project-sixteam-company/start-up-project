package com.startup.domain.ai.dto;

public record ScoringResult(
        int totalScore,
        int culpritScore,
        boolean culpritCorrect,
        int methodScore,
        int motiveScore,
        int coverUpScore,
        int evidenceScore,
        int evidenceMatchCount
) {}
