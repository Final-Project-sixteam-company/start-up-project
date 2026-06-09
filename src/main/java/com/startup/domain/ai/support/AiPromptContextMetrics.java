package com.startup.domain.ai.support;

public record AiPromptContextMetrics(
        int systemRuleTokens,
        int scenarioContextTokens,
        int npcProfileTokens,
        int evidenceContextTokens,
        int historyTokens,
        int questionTokens,
        int promptCharLength,
        int historyTurns,
        int includedEvidenceCount
) {
}
