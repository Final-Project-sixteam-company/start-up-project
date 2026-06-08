package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.Hint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HintRepository extends JpaRepository<Hint, Long> {

    // 시나리오의 모든 힌트를 레벨 순으로 조회
    List<Hint> findAllByScenarioIdOrderByHintLevel(Long scenarioId);

    int countByScenarioId(Long scenarioId);

    //여러 시나리오의 힌트 수를 한번에 조회
    @Query("SELECT h.scenarioId, COUNT(h) FROM Hint h WHERE h.scenarioId IN :scenarioIds GROUP BY h.scenarioId")
    List<Object[]> countByScenarioIdIn(@Param("scenarioIds") List<Long> scenarioIds);

    @Query("SELECT COALESCE(MAX(h.hintLevel), 0) FROM Hint h WHERE h.scenarioId = :scenarioId")
    Integer findMaxHintLevelByScenarioId(@Param("scenarioId") Long scenarioId);
}
