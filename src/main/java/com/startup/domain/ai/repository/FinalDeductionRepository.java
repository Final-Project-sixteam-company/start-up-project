package com.startup.domain.ai.repository;

import com.startup.domain.ai.entity.FinalDeduction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FinalDeductionRepository extends JpaRepository<FinalDeduction, Long> {

    Optional<FinalDeduction> findByPlaySessionId(Long playSessionId);

    boolean existsByPlaySessionId(Long playSessionId);
}
