package com.startup.domain.scenario.dto;

import com.startup.domain.scenario.entity.Solution;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CustomSolutionResponse {
    private Long solutionId;
    private Long scenarioId;
    private Long culpritSuspectId;
    private String motive;
    private String method;
    private String coverUp;
    private String fullExplanation;
    private List<Long> keyEvidenceIds;

    public static CustomSolutionResponse from(Solution solution) {
        return CustomSolutionResponse.builder()
                .solutionId(solution.getId())
                .scenarioId(solution.getScenarioId())
                .culpritSuspectId(solution.getCulpritSuspectId())
                .motive(solution.getMotive())
                .method(solution.getMethod())
                .coverUp(solution.getCoverUp())
                .fullExplanation(solution.getFullExplanation())
                .keyEvidenceIds(solution.parseKeyEvidenceIds())
                .build();
    }
}
