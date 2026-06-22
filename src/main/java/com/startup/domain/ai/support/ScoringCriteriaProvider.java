package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.ScoringCriteria;

public interface ScoringCriteriaProvider {

    ScoringCriteria getByCriteria(Long scenarioId);
}
