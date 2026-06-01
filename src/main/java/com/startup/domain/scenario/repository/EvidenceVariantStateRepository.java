package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.EvidenceVariantState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvidenceVariantStateRepository extends JpaRepository<EvidenceVariantState, Long> {

    Optional<EvidenceVariantState> findByVariantIdAndEvidenceId(Long variantId, Long evidenceId);

    List<EvidenceVariantState> findAllByVariantId(Long variantId);
}
