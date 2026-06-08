package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.ScenarioLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScenarioLocationRepository extends JpaRepository<ScenarioLocation, Long> {

    Optional<ScenarioLocation> findById(Long id);

    List<ScenarioLocation> findAllByScenarioIdOrderBySortOrder(Long scenarioId);

    Optional<ScenarioLocation> findByScenarioIdAndCode(Long scenarioId, String code);

    @Query("SELECT COALESCE(MAX(l.sortOrder), 0) FROM ScenarioLocation l WHERE l.scenarioId = :scenarioId")
    Integer findMaxSortOrderByScenarioId(@Param("scenarioId") Long scenarioId);
}
