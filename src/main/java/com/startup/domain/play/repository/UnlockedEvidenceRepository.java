package com.startup.domain.play.repository;

import com.startup.domain.play.entity.UnlockedEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UnlockedEvidenceRepository extends JpaRepository<UnlockedEvidence, Long> {

    List<UnlockedEvidence> findAllByPlaySessionId(Long playSessionId);

    int countByPlaySessionId(Long playSessionId);

    boolean existsByPlaySessionIdAndEvidenceId(Long playSessionId, Long evidenceId);

    Optional<UnlockedEvidence> findByPlaySessionIdAndEvidenceId(Long playSessionId, Long evidenceId);

    @Query(value = "SELECT * FROM unlocked_evidences " +
            "WHERE play_session_id = :sessionId AND evidence_id = :evidenceId FOR UPDATE", nativeQuery = true)
    Optional<UnlockedEvidence> findByPlaySessionIdAndEvidenceIdForUpdate(
            @Param("sessionId") Long sessionId,
            @Param("evidenceId") Long evidenceId
    );

    @Modifying
    @Query(value = "INSERT IGNORE INTO unlocked_evidences (play_session_id, evidence_id, unlocked_reason, unlocked_at) " +
            "VALUES (:sessionId, :evidenceId, :reason, NOW())", nativeQuery = true)
    int insertIgnoreUnlockedEvidence(@Param("sessionId") Long sessionId, @Param("evidenceId") Long evidenceId, @Param("reason") String reason);
}
