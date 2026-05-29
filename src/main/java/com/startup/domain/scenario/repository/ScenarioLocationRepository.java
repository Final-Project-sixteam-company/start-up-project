package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.ScenarioLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScenarioLocationRepository extends JpaRepository<ScenarioLocation, Long> {

    Optional<ScenarioLocation> findById(Long id);

    List<ScenarioLocation> findAllByScenarioIdOrderBySortOrder(Long scenarioId);
}
