package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.Victim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VictimRepository extends JpaRepository<Victim, Long> {

    Optional<Victim> findFirstByScenarioId(Long scenarioId);

    Optional<Victim> findByScenarioId(Long scenarioId);
}
