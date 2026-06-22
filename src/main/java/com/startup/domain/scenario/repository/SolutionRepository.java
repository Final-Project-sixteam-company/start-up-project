package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.Solution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SolutionRepository extends JpaRepository<Solution, Long> {

    // 시나리오 ID로 정답 단건 조회 (1:1 관계이므로 Optional 반환)
    Optional<Solution> findByScenarioId(Long scenarioId);

    // 발행 정합성 검증용 (정답이 설정되어 있는지 여부만 확인)
    boolean existsByScenarioId(Long scenarioId);
}
