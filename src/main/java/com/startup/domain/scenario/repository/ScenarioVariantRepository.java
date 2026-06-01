package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.ScenarioVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ScenarioVariantRepository extends JpaRepository<ScenarioVariant, Long> {

    //MVP: 시나리오의 활성 variant 1개 조회 (SECRETARY 고정)
    Optional<ScenarioVariant> findFirstByScenarioIdAndIsActiveTrueOrderBySortOrderAsc(Long scenarioId);

    // 특정 시나리오의 모든 Variant를 비활성화하는 벌크 업데이트
    @Modifying
    @Query("UPDATE ScenarioVariant v SET v.isActive = false WHERE v.scenarioId = :scenarioId")
    void deactivateAllByScenarioId(@Param("scenarioId") Long scenarioId);
}
