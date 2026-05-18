package com.startup.domain.ai.service;

import java.util.List;

public interface AiDeductionScorer {

    int score(Long sessionId, Long selectedCulpritId, String motiveText,
              String methodText, String coverUpText, List<Long> selectedEvidenceIds);
}
