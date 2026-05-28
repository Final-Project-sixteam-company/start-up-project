package com.startup.domain.play.repository;

import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.enums.PlaySessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaySessionRepository extends JpaRepository<PlaySession, Long> {

    // 같은 사용자가 같은 시나리오에서 진행 중인 세션이 있는지 확인
    Optional<PlaySession> findByUserIdAndScenarioIdAndStatus(Long userId, Long scenarioId, PlaySessionStatus status);
}
