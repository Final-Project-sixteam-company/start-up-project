package com.startup.domain.play.repository;

import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.enums.PlaySessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlaySessionRepository extends JpaRepository<PlaySession, Long> {

    // 같은 사용자가 같은 시나리오에서 진행 중인 세션이 있는지 확인
    Optional<PlaySession> findByUserIdAndScenarioIdAndStatus(Long userId, Long scenarioId, PlaySessionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ps from PlaySession ps where ps.id = :sessionId")
    Optional<PlaySession> findByIdForUpdate(@Param("sessionId") Long sessionId);
}
