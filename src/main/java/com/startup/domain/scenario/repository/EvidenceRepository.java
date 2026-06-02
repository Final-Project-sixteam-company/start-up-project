package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    List<Evidence> findAllByScenarioIdOrderBySortOrder(Long scenarioId);

    List<Evidence> findAllByScenarioIdAndIsInitialPublicTrue(Long scenarioId);

    int countByScenarioId(Long scenarioId);

    Optional<Evidence> findByScenarioIdAndCode(Long scenarioId, String code);

    //여러 시나리오의 증거 수를 한번에 조회
    @Query("SELECT e.scenarioId, COUNT(e) FROM Evidence e WHERE e.scenarioId IN :scenarioIds GROUP BY e.scenarioId")
    List<Object[]> countByScenarioIdIn(@Param("scenarioIds") List<Long> scenarioIds);
}
