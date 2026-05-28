package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.EvidenceSuspect;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvidenceSuspectRepository extends JpaRepository<EvidenceSuspect, Long> {

    List<EvidenceSuspect> findAllByEvidenceIdIn(List<Long> evidenceIds);

    List<EvidenceSuspect> findAllBySuspectIdIn(List<Long> suspectIds);
}
