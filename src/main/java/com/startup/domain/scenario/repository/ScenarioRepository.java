package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {
    
    // PUBLISHED 시나리오 중, 누구나 볼 수 있는 권한(PUBLIC, OFFICIAL)인 목록만 조회
    Page<Scenario> findAllByStatusAndVisibilityIn(ScenarioStatus status, List<ScenarioVisibility> visibilities, Pageable pageable);
}
