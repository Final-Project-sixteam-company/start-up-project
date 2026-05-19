package com.startup.domain.ai.dto;

import java.util.List;

public record SolutionInfo(
        Long culpritSuspectId,
        String motive,
        String method,
        String coverUp,
        String fullExplanation,
        List<Long> keyEvidenceIds
) {}
