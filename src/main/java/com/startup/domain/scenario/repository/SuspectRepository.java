package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.Suspect;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SuspectRepository extends JpaRepository<Suspect, Long> {

    List<Suspect> findAllByScenarioIdOrderBySortOrder(Long scenarioId);

    int countByScenarioId(Long scenarioId);
}
