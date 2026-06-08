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

    // 시나리오 내 장소별 전체 증거 개수를 한 번에 조회합니다. (N+1 방지)
    @Query("SELECT e.locationId, COUNT(e) FROM Evidence e WHERE e.scenarioId = :scenarioId AND e.locationId IS NOT NULL GROUP BY e.locationId")
    List<Object[]> countByLocationIdForScenario(@Param("scenarioId") Long scenarioId);

    long countByIdInAndScenarioId(List<Long> ids, Long scenarioId);

    @Query("SELECT COALESCE(MAX(e.sortOrder), 0) FROM Evidence e WHERE e.scenarioId = :scenarioId")
    Integer findMaxSortOrderByScenarioId(@Param("scenarioId") Long scenarioId);

}
