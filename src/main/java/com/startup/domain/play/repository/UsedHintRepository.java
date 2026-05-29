package com.startup.domain.play.repository;

import com.startup.domain.play.entity.UsedHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UsedHintRepository extends JpaRepository<UsedHint, Long> {

    List<UsedHint> findAllByPlaySessionId(Long playSessionId);

    boolean existsByPlaySessionIdAndHintId(Long playSessionId, Long hintId);

    Optional<UsedHint> findByPlaySessionIdAndHintId(Long playSessionId, Long hintId);

    int countByPlaySessionId(Long playSessionId);

    @Modifying
    @Query(value = "INSERT IGNORE INTO used_hints (play_session_id, hint_id, used_at) VALUES (:sessionId, :hintId, :used_at)", nativeQuery = true)
    int insertIgnoreUsedHint(@Param("sessionId") Long sessionId, @Param("hintId") Long hintId, @Param("usedAt") LocalDateTime usedAt);
}
