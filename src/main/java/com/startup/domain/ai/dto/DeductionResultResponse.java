package com.startup.domain.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "채점 결과/해설 응답")
public record DeductionResultResponse(
        Long sessionId,
        Integer score,
        String grade,
        CorrectCulpritDto correctCulprit,
        MatchedDto matched,
        List<String> matchedParts,
        List<String> missedParts,
        String feedback,
        String fullExplanation,
        List<EvidenceDto> keyEvidences,
        List<ScenarioDto> nextRecommendedScenarios
) {
    public record CorrectCulpritDto(Long suspectId, String name, String role) {}

    public record MatchedDto(boolean culprit, boolean motive, boolean method,
                             boolean coverUp, int keyEvidences) {}

    public record EvidenceDto(Long evidenceId, String title) {}

    public record ScenarioDto(Long scenarioId, String title) {}
}
