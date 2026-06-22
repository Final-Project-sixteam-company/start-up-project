package com.startup.domain.ai.support;

import org.springframework.stereotype.Component;

@Component
public class MockHintPenaltyReader implements HintPenaltyReader {

    @Override
    public int getTotalPenalty(Long sessionId) {
        return 0;
    }
}
