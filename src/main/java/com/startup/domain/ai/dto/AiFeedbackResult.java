package com.startup.domain.ai.dto;

import java.util.List;

public record AiFeedbackResult(
        List<String> matchedParts,
        List<String> missedParts,
        String feedback
) {}
