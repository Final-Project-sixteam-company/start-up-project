package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.ScenarioVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScenarioVariantRepository extends JpaRepository<ScenarioVariant, Long> {

    //MVP: 시나리오의 활성 variant 1개 조회 (SECRETARY 고정)
    Optional<ScenarioVariant> findFirstByScenarioIdAndIsActiveTrueOrderBySortOrderAsc(Long scenarioId);

    List<ScenarioVariant> findAllByScenarioIdAndIsActiveTrueOrderBySortOrderAsc(Long scenarioId);

    Optional<ScenarioVariant> findByScenarioIdAndCode(Long scenarioId, String code);
}
