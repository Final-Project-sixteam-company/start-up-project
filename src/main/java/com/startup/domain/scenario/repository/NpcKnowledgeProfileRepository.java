package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.NpcKnowledgeProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NpcKnowledgeProfileRepository extends JpaRepository<NpcKnowledgeProfile, Long> {

    Optional<NpcKnowledgeProfile> findBySuspectId(Long suspectId);

    List<NpcKnowledgeProfile> findAllByScenarioId(Long scenarioId);
}
