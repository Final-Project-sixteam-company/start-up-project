package com.startup.domain.play.repository;

import com.startup.domain.play.entity.UnlockedEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnlockedEvidenceRepository extends JpaRepository<UnlockedEvidence, Long> {

    List<UnlockedEvidence> findAllByPlaySessionId(Long playSessionId);

    int countByPlaySessionId(Long playSessionId);

    boolean existsByPlaySessionIdAndEvidenceId(Long playSessionId, Long evidenceId);
}
