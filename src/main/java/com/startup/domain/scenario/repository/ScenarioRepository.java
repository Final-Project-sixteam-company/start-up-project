package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {
}
