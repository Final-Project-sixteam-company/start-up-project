package com.startup.domain.community.repository;

import com.startup.domain.community.entity.ScenarioReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScenarioReviewRepository extends JpaRepository<ScenarioReview, Long> {

    boolean existsByUserIdAndScenarioId(Long userId, Long scenarioId);

    @Query("SELECT r FROM ScenarioReview r JOIN FETCH r.user WHERE r.scenarioId = :scenarioId")
    Page<ScenarioReview> findAllByScenarioId(@Param("scenarioId") Long scenarioId, Pageable pageable);

    @Query("SELECT r FROM ScenarioReview r JOIN FETCH r.user WHERE r.id = :reviewId")
    java.util.Optional<ScenarioReview> findWithUserById(@Param("reviewId") Long reviewId);
}
