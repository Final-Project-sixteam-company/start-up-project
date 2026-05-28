package com.startup.domain.play.support;

import com.startup.domain.ai.support.PlaySessionReader;
import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.error.PlayErrorCode;
import com.startup.domain.play.error.PlayException;
import com.startup.domain.play.repository.PlaySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@RequiredArgsConstructor
public class DefaultPlaySessionReader implements PlaySessionReader {

    private final PlaySessionRepository playSessionRepository;

    @Override
    public boolean isPlaying(Long sessionId) {
        return getSessionOrThrow(sessionId).isPlaying();
    }

    @Override
    public Long getScenarioId(Long sessionId) {
        return getSessionOrThrow(sessionId).getScenarioId();
    }

    @Override
    public Long getOwnerUserId(Long sessionId) {
        return getSessionOrThrow(sessionId).getUserId();
    }

    private PlaySession getSessionOrThrow(Long sessionId) {
        return playSessionRepository.findById(sessionId)
                .orElseThrow(() -> new PlayException(PlayErrorCode.SESSION_NOT_FOUND));
    }
}
