package com.startup.domain.community.repository;

import com.startup.domain.community.entity.ScenarioBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ScenarioBookmarkRepository extends JpaRepository<ScenarioBookmark, Long> {

    boolean existsByUserIdAndScenarioId(Long userId, Long scenarioId);

    Optional<ScenarioBookmark> findByUserIdAndScenarioId(Long userId, Long scenarioId);

    @Query("SELECT b.scenarioId FROM ScenarioBookmark b WHERE b.userId = :userId AND b.scenarioId IN :scenarioIds")
    Set<Long> findScenarioIdsByUserIdAndScenarioIdIn(@Param("userId") Long userId, @Param("scenarioIds") List<Long> scenarioIds);
}
