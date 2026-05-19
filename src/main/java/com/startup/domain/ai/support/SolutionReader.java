package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.SolutionInfo;

public interface SolutionReader {

    SolutionInfo findByScenarioId(Long scenarioId);
}
