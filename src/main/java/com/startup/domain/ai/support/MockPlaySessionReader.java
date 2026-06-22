package com.startup.domain.ai.support;

import com.startup.common.auth.MockUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MockPlaySessionReader implements PlaySessionReader {

    private final MockUserProvider mockUserProvider;

    @Override
    public boolean isPlaying(Long sessionId) {
        return true;
    }

    @Override
    public Long getScenarioId(Long sessionId) {
        return 1L;
    }

    @Override
    public Long getOwnerUserId(Long sessionId) {
        return mockUserProvider.currentUserId();
    }

    @Override
    public Long getScenarioVariantId(Long sessionId) {
        return null; // Mock 환경에서는 null 반환
    }
}
