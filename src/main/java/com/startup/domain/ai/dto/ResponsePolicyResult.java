package com.startup.domain.ai.dto;

import java.util.List;

public record ResponsePolicyResult(
        String conditionKey,
        String policyText,
        List<String> allowedFacts,
        List<String> forbiddenFacts,
        String tone
) {
    private static final ResponsePolicyResult HARDCODED_FALLBACK = new ResponsePolicyResult(
            "HARDCODED_FALLBACK",
            "질문에 대해 모호하게 답한다. 구체적인 사실을 확인해 주지 않는다.",
            List.of(),
            List.of(),
            "조심스러운 말투"
    );

    public static ResponsePolicyResult hardcodedFallback() {
        return HARDCODED_FALLBACK;
    }
}
