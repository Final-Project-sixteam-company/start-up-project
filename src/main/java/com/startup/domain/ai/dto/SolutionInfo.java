package com.startup.domain.ai.dto;

import java.util.List;
import java.util.Map;

public record SolutionInfo(
        Long culpritSuspectId,
        String culpritName,
        String culpritRole,
        String motive,
        String method,
        String coverUp,
        String fullExplanation,
        List<Long> keyEvidenceIds,
        Map<Long, String> evidenceTitles
) {}
