package com.startup.domain.community.repository;

import com.startup.domain.community.entity.ScenarioReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScenarioReviewRepository extends JpaRepository<ScenarioReview, Long> {

    boolean existsByUserIdAndScenarioId(Long userId, Long scenarioId);
}
