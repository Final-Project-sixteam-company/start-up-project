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

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {

    Optional<Scenario> findByCodeAndVersion(String code, String version);
    
    // PUBLISHED 시나리오 중, 누구나 볼 수 있는 권한(PUBLIC, OFFICIAL)인 목록만 조회
    Page<Scenario> findAllByStatusAndVisibilityIn(ScenarioStatus status, List<ScenarioVisibility> visibilities, Pageable pageable);

    //db에서 직접 원자적으로 플레이 카운트를 1 증가시키는 쿼리
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Scenario s SET s.playCount = s.playCount + 1 WHERE s.id = :id")
    void incrementPlayCount(@Param("id") Long id);

}
