package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.ScenarioAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScenarioAssetRepository extends JpaRepository<ScenarioAsset, Long> {

    Optional<ScenarioAsset> findByAssetKey(String assetKey);

    List<ScenarioAsset> findAllByScenarioId(Long scenarioId);
}
