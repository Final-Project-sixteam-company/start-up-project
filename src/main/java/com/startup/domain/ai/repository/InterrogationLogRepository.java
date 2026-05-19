package com.startup.domain.ai.repository;

import com.startup.domain.ai.entity.InterrogationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterrogationLogRepository extends JpaRepository<InterrogationLog, Long> {

    List<InterrogationLog> findTop5ByPlaySessionIdAndSuspectIdOrderByCreatedAtDesc(
            Long playSessionId, Long suspectId);

    int countByPlaySessionIdAndSuspectId(Long playSessionId, Long suspectId);
}
