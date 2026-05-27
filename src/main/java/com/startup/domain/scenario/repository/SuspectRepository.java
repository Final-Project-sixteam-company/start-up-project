package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.Suspect;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SuspectRepository extends JpaRepository<Suspect, Long> {

    List<Suspect> findAllByScenarioIdOrderBySortOrder(Long scenarioId);

    int countByScenarioId(Long scenarioId);

    Optional<Suspect> findByIdAndScenarioId(Long id, Long scenarioId);
}
