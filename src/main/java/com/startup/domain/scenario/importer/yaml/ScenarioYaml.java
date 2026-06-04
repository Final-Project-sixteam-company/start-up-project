package com.startup.domain.scenario.importer.yaml;

import java.util.List;
import java.util.Map;

public record ScenarioYaml(
        MetadataYaml metadata,
        ScenarioSectionYaml scenario,
        VictimYaml victim,
        List<LocationYaml> locations,
        List<CharacterYaml> characters,
        List<EvidenceYaml> evidences,
        List<TimelineEventYaml> timelineEvents,
        List<EvidenceVariantStateYaml> evidenceVariantStates,
        List<VariantYaml> variants,
        List<UnlockRuleYaml> unlockRules,
        List<NpcPolicyYaml> npcPolicies,
        Map<String, Object> scoring,
        List<AssetYaml> assets
) {

    public record MetadataYaml(
            Integer schemaVersion,
            String contentStatus,
            String locale,
            String sourceCanonRef,
            String workingBriefRef,
            String canonicalCodeRef,
            String assetMapRef,
            String assetKeyPattern,
            String draftScope
    ) {
    }

    public record ScenarioSectionYaml(
            String code,
            String version,
            String title,
            String description,
            String synopsis,
            String genre,
            String difficulty,
            Integer estimatedPlayTimeMinutes,
            String scenarioType,
            String visibility,
            String status,
            String culpritMode,
            String deductionMode,
            String mapMode,
            String evidenceMode,
            String coverAssetKey,
            String mapAssetKey
    ) {
    }

    public record VictimYaml(
            String code,
            String name,
            String roleLabel,
            String publicProfile,
            String deathLocationCode,
            String discoveredAtText,
            String publicCauseOfDeathText,
            String portraitAssetKey
    ) {
    }

    public record LocationYaml(
            String code,
            String name,
            String floor,
            String description,
            String imageAssetKey,
            Integer mapX,
            Integer mapY,
            Integer sortOrder
    ) {
    }

    public record CharacterYaml(
            String code,
            String name,
            String roleLabel,
            String characterType,
            Boolean culpritEligible,
            String publicProfile,
            String publicAlibi,
            String personalityTone,
            String portraitAssetKey,
            Integer sortOrder
    ) {
    }

    public record EvidenceYaml(
            String code,
            String title,
            String category,
            String unlockPhase,
            String locationCode,
            String baseRole,
            String oneLine,
            String baseDetail,
            String imageAssetKey,
            String thumbnailAssetKey,
            List<String> relatedCharacterCodes,
            List<String> tags,
            Integer sortOrder
    ) {
    }

    public record TimelineEventYaml(
            String code,
            Integer eventOrder,
            String eventTime,
            String title,
            String description,
            String eventType,
            String visibility,
            Boolean isTrueEvent,
            String locationCode,
            String relatedEvidenceCode,
            String relatedCharacterCode
    ) {
    }

    public record EvidenceVariantStateYaml(
            String variantCode,
            String evidenceCode,
            String role,
            String detailOverride,
            String detailAppend,
            List<String> contradictionKeys,
            List<String> proofDimensions
    ) {
    }

    public record VariantYaml(
            String code,
            Boolean enabled,
            Boolean mvpActiveOption,
            Integer weight,
            String culpritCode,
            String culpritName,
            String culpritRole,
            String mainMethodLayer,
            String fatalLayerSummary,
            String directCause,
            Map<String, Object> coreTimeWindow,
            Map<String, Object> solution,
            List<String> misleadingEvidenceCodes
    ) {
    }

    public record UnlockRuleYaml(
            String evidenceCode,
            String unlockType,
            UnlockConditionYaml condition,
            Integer sortOrder
    ) {
    }

    public record UnlockConditionYaml(
            String requiredPhase,
            List<String> requiredEvidenceCodes,
            String requiredCharacterCode,
            String requiredInterrogationTopic,
            Boolean hintFallbackAllowed
    ) {
    }

    public record NpcPolicyYaml(
            String characterCode,
            String publicAlibi,
            PromptSafeKnowledgeYaml promptSafeKnowledge,
            List<NpcStagePolicyYaml> stagePolicies,
            List<NpcEvidenceReactionPolicyYaml> evidenceReactionPolicies
    ) {
    }

    public record PromptSafeKnowledgeYaml(
            String selfRole,
            List<String> directKnowledge,
            List<String> inferredKnowledge,
            List<String> forbiddenKnowledge
    ) {
    }

    public record NpcStagePolicyYaml(
            String stage,
            String policyText,
            List<String> allowedFacts,
            List<String> forbiddenFacts,
            String tone
    ) {
    }

    public record NpcEvidenceReactionPolicyYaml(
            String evidenceCode,
            String policyText,
            List<String> allowedFacts,
            List<String> forbiddenFacts,
            String tone,
            Integer priority
    ) {
    }

    public record AssetYaml(
            String assetKey,
            String type,
            String targetKind,
            String targetCode,
            String s3ObjectKey,
            String contentType,
            String sourceLocalFile,
            String sourceStatus,
            String altText
    ) {
    }
}
