package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.Suspect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SuspectRepository extends JpaRepository<Suspect, Long> {

    List<Suspect> findAllByScenarioIdOrderBySortOrder(Long scenarioId);

    int countByScenarioId(Long scenarioId);

    Optional<Suspect> findByIdAndScenarioId(Long id, Long scenarioId);

    Optional<Suspect> findByScenarioIdAndCode(Long scenarioId, String code);

    //여러 시나리오의 용의자 수를 한번에 조회
    @Query("SELECT s.scenarioId, COUNT(s) FROM Suspect s WHERE s.scenarioId IN :scenarioIds GROUP BY s.scenarioId")
    List<Object[]> countByScenarioIdIn(@Param("scenarioIds") List<Long> scenarioIds);

    @Query("SELECT COALESCE(MAX(s.sortOrder), 0) FROM Suspect s WHERE s.scenarioId = :scenarioId")
    Integer findMaxSortOrderByScenarioId(@Param("scenarioId") Long scenarioId);

    // 증거/정답 검증용 메서드
    List<Suspect> findAllByIdInAndScenarioId(List<Long> ids, Long scenarioId);
}
