package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.TimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimelineEventRepository extends JpaRepository<TimelineEvent, Long> {

    List<TimelineEvent> findAllByScenarioIdOrderByEventOrder(Long scenarioId);
}
