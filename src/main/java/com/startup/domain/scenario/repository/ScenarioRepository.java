package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioVisibility;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScenarioRepository extends JpaRepository<Scenario, Long>, ScenarioRepositoryCustom {

    Optional<Scenario> findByCodeAndVersion(String code, String version);
    
    // PUBLISHED 시나리오 중, 누구나 볼 수 있는 권한(PUBLIC, OFFICIAL)인 목록만 조회
    Page<Scenario> findAllByStatusAndVisibilityIn(ScenarioStatus status, List<ScenarioVisibility> visibilities, Pageable pageable);

    // 내 시나리오 목록 조회 (삭제된 것 제외)
    Page<Scenario> findAllByCreatorIdAndStatusNot(Long creatorId, ScenarioStatus status, Pageable pageable);

    // 내가 북마크한 시나리오 목록 조회 (삭제/숨김 처리된 것 방어)
    @Query(value = "SELECT s FROM Scenario s JOIN ScenarioBookmark b ON s.id = b.scenarioId " +
                   "WHERE b.userId = :userId AND s.status = :status AND s.visibility IN :visibilities",
           countQuery = "SELECT COUNT(s) FROM Scenario s JOIN ScenarioBookmark b ON s.id = b.scenarioId " +
                        "WHERE b.userId = :userId AND s.status = :status AND s.visibility IN :visibilities")
    Page<Scenario> findBookmarkedScenarios(@Param("userId") Long userId,
                                           @Param("status") ScenarioStatus status,
                                           @Param("visibilities") List<ScenarioVisibility> visibilities,
                                           Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Scenario s WHERE s.id = :id")
    Optional<Scenario> findByIdForUpdate(@Param("id") Long id);

    //db에서 직접 원자적으로 플레이 카운트를 1 증가시키는 쿼리
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Scenario s SET s.playCount = s.playCount + 1 WHERE s.id = :id")
    void incrementPlayCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Scenario s SET s.ratingCount = "
         + "(SELECT COUNT(r) FROM ScenarioReview r WHERE r.scenarioId = s.id), "
         + "s.averageRating = COALESCE("
         + "(SELECT AVG(CAST(r.rating AS double)) FROM ScenarioReview r WHERE r.scenarioId = s.id), 0.0) "
         + "WHERE s.id = :scenarioId")
    void recalculateRating(@Param("scenarioId") Long scenarioId);
}
