package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.EvidenceUnlockRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvidenceUnlockRuleRepository extends JpaRepository<EvidenceUnlockRule, Long> {

    Optional<EvidenceUnlockRule> findByEvidenceId(Long evidenceId);

    List<EvidenceUnlockRule> findAllByScenarioIdOrderBySortOrder(Long scenarioId);
}
