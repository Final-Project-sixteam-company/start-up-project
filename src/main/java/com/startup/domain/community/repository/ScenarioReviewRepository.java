package com.startup.domain.community.repository;

import com.startup.domain.community.entity.ScenarioReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ScenarioReviewRepository extends JpaRepository<ScenarioReview, Long> {

    boolean existsByUserIdAndScenarioId(Long userId, Long scenarioId);

    @Query(
            value = "SELECT r FROM ScenarioReview r JOIN FETCH r.user WHERE r.scenarioId = :scenarioId",
            countQuery = "SELECT count(r) FROM ScenarioReview r WHERE r.scenarioId = :scenarioId"
    )
    Page<ScenarioReview> findAllByScenarioId(@Param("scenarioId") Long scenarioId, Pageable pageable);

    @Query(
            value = "SELECT r FROM ScenarioReview r JOIN FETCH r.user WHERE r.scenarioId = :scenarioId AND r.isSpoiler = false",
            countQuery = "SELECT count(r) FROM ScenarioReview r WHERE r.scenarioId = :scenarioId AND r.isSpoiler = false"
    )
    Page<ScenarioReview> findAllByScenarioIdAndIsSpoilerFalse(@Param("scenarioId") Long scenarioId, Pageable pageable);

    @Query("SELECT r FROM ScenarioReview r JOIN FETCH r.user WHERE r.id = :reviewId")
    Optional<ScenarioReview> findWithUserById(@Param("reviewId") Long reviewId);
}
