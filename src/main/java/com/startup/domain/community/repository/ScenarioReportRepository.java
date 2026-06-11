package com.startup.domain.community.repository;

import com.startup.domain.community.entity.ScenarioReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScenarioReportRepository extends JpaRepository<ScenarioReport, Long> {
    
    boolean existsByReporterIdAndScenarioId(Long reporterId, Long scenarioId);
}
