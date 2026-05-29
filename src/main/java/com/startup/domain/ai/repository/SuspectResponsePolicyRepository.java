package com.startup.domain.ai.repository;

import com.startup.domain.ai.entity.SuspectResponsePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SuspectResponsePolicyRepository extends JpaRepository<SuspectResponsePolicy, Long> {

    List<SuspectResponsePolicy> findAllBySuspectId(Long suspectId);

    List<SuspectResponsePolicy> findAllBySuspectIdIn(List<Long> suspectIds);
}
