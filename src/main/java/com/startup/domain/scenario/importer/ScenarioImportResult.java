package com.startup.domain.scenario.importer;

public record ScenarioImportResult(
        Status status,
        Long scenarioId,
        String scenarioCode,
        String version,
        int locationCount,
        int characterCount,
        int evidenceCount,
        int variantCount,
        int evidenceVariantStateCount,
        int unlockRuleCount,
        int npcPolicyCount,
        int timelineEventCount,
        int assetCount
) {

    public enum Status {
        IMPORTED,
        SKIPPED
    }

    public static ScenarioImportResult skipped(Long scenarioId, String scenarioCode, String version) {
        return new ScenarioImportResult(
                Status.SKIPPED,
                scenarioId,
                scenarioCode,
                version,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0
        );
    }
}
