package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    List<Evidence> findAllByScenarioIdOrderBySortOrder(Long scenarioId);

    List<Evidence> findAllByScenarioIdAndIsInitialPublicTrue(Long scenarioId);

    int countByScenarioId(Long scenarioId);
}
