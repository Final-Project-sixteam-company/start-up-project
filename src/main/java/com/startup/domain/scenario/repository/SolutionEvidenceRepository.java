package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.SolutionEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolutionEvidenceRepository extends JpaRepository<SolutionEvidence, Long> {

    List<SolutionEvidence> findAllBySolutionId(Long solutionId);
    void deleteAllBySolutionId(Long solutionId);
}
