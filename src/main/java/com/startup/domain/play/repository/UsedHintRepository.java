package com.startup.domain.play.repository;

import com.startup.domain.play.entity.UsedHint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsedHintRepository extends JpaRepository<UsedHint, Long> {

    List<UsedHint> findAllByPlaySessionId(Long playSessionId);

    boolean existsByPlaySessionIdAndHintId(Long playSessionId, Long hintId);

    int countByPlaySessionId(Long playSessionId);
}
