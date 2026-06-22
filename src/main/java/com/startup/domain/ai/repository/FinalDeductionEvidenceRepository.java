package com.startup.domain.ai.repository;

import com.startup.domain.ai.entity.FinalDeductionEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinalDeductionEvidenceRepository extends JpaRepository<FinalDeductionEvidence, Long> {

    List<FinalDeductionEvidence> findAllByFinalDeductionId(Long finalDeductionId);
}
