package com.startup.domain.ai.support;

import org.springframework.stereotype.Component;

@Component
public class MockPlaySessionReader implements PlaySessionReader {

    @Override
    public boolean isPlaying(Long sessionId) {
        return true;
    }

    @Override
    public Long getScenarioId(Long sessionId) {
        return 1L;
    }
}
