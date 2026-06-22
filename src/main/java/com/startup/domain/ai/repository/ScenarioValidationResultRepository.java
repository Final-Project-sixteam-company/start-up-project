package com.startup.domain.ai.repository;

import com.startup.domain.ai.entity.ScenarioValidationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScenarioValidationResultRepository extends JpaRepository<ScenarioValidationResult, Long> {

    Optional<ScenarioValidationResult> findTopByScenarioIdOrderByCheckedAtDescIdDesc(Long scenarioId);
}
