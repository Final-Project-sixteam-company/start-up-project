package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.TimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimelineEventRepository extends JpaRepository<TimelineEvent, Long> {

    List<TimelineEvent> findAllByScenarioIdOrderByEventOrder(Long scenarioId);

    // 특정 증거와 연관된 타임라인 사건을 순서대로 조회
    List<TimelineEvent> findAllByRelatedEvidenceIdOrderByEventOrder(Long relatedEvidenceId);
}
