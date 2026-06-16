package com.startup.domain.play.support;

import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.enums.PlaySessionStatus;
import com.startup.domain.play.repository.PlaySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ActivePlaySessionLookup {

    private final PlaySessionRepository playSessionRepository;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<PlaySession> findPlaying(Long userId, Long scenarioId) {
        return playSessionRepository.findByUserIdAndScenarioIdAndStatus(
                userId,
                scenarioId,
                PlaySessionStatus.PLAYING
        );
    }
}
