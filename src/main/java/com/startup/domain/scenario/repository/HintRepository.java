package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.Hint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HintRepository extends JpaRepository<Hint, Long> {

    // 시나리오의 모든 힌트를 레벨 순으로 조회
    List<Hint> findAllByScenarioIdOrderByHintLevel(Long scenarioId);

    int countByScenarioId(Long scenarioId);
}
