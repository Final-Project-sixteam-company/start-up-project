package com.startup.domain.ai.client;

import com.startup.domain.ai.enums.AiFeatureType;

public record AiCallContext(
        AiFeatureType featureType,
        String promptVersion,
        Long scenarioId,
        Long sessionId,
        Long suspectId,
        String npcCode
) {

    public static AiCallContext unknown() {
        return new AiCallContext(AiFeatureType.UNKNOWN, "unknown", null, null, null, null);
    }
}
