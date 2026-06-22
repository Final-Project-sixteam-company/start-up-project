package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.entity.VariantSolution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VariantSolutionRepository extends JpaRepository<VariantSolution, Long> {

    Optional<VariantSolution> findByVariantId(Long variantId);
}
