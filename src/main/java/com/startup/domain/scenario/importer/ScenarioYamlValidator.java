package com.startup.domain.scenario.importer;

import com.startup.domain.scenario.importer.yaml.ScenarioYaml;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class ScenarioYamlValidator {

    private static final Pattern ASSET_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9._/-]+$");
    private static final Pattern LOCAL_PATH_PATTERN = Pattern.compile("^[A-Za-z]:\\\\|.*\\\\.*");
    private static final Set<String> TIMELINE_EVENT_VISIBILITIES = Set.of("PUBLIC");

    public List<String> validate(ScenarioYaml yaml) {
        List<String> violations = new ArrayList<>();

        if (yaml == null) {
            return List.of("YAML content is empty.");
        }

        validateMetadata(yaml, violations);
        validateScenario(yaml, violations);
        validatePublishedRootSections(yaml, violations);

        Set<String> locationCodes = collectCodes(yaml.locations(), ScenarioYaml.LocationYaml::code, "locations", violations);
        Map<String, ScenarioYaml.CharacterYaml> charactersByCode = collectCodeMap(
                yaml.characters(), ScenarioYaml.CharacterYaml::code, "characters", violations);
        Set<String> characterCodes = charactersByCode.keySet();
        Set<String> evidenceCodes = collectCodes(yaml.evidences(), ScenarioYaml.EvidenceYaml::code, "evidences", violations);
        Set<String> variantCodes = collectCodes(yaml.variants(), ScenarioYaml.VariantYaml::code, "variants", violations);
        String scenarioCode = yaml.scenario() == null ? null : yaml.scenario().code();
        String victimCode = yaml.victim() == null ? null : yaml.victim().code();

        validateLocations(yaml, violations);
        validateVictim(yaml, locationCodes, violations);
        validateEvidenceReferences(yaml, locationCodes, characterCodes, violations);
        validateTimelineEvents(yaml, locationCodes, evidenceCodes, characterCodes, violations);
        validateVariantReferences(yaml, charactersByCode, evidenceCodes, violations);
        validatePublishedVariants(yaml, violations);
        validateEvidenceVariantStates(yaml, evidenceCodes, variantCodes, violations);
        validateUnlockRules(yaml, evidenceCodes, characterCodes, violations);
        validateNpcPolicies(yaml, evidenceCodes, characterCodes, violations);
        validateAssets(yaml, scenarioCode, victimCode, locationCodes, characterCodes,
                evidenceCodes, variantCodes, violations);

        return violations;
    }

    public void validateOrThrow(ScenarioYaml yaml) {
        List<String> violations = validate(yaml);
        if (!violations.isEmpty()) {
            throw new ScenarioImportException(violations);
        }
    }

    private void validateScenario(ScenarioYaml yaml, List<String> violations) {
        if (yaml.scenario() == null) {
            violations.add("scenario section is required.");
            return;
        }
        requireText(yaml.scenario().code(), "scenario.code", violations);
        requireText(yaml.scenario().version(), "scenario.version", violations);
        requireText(yaml.scenario().title(), "scenario.title", violations);
        requireText(yaml.scenario().status(), "scenario.status", violations);
        requireText(yaml.scenario().visibility(), "scenario.visibility", violations);
        requireText(yaml.scenario().scenarioType(), "scenario.scenarioType", violations);
    }

    private void validateVictim(ScenarioYaml yaml, Set<String> locationCodes, List<String> violations) {
        if (yaml.victim() == null) {
            violations.add("victim section is required.");
            return;
        }
        requireText(yaml.victim().code(), "victim.code", violations);
        requireText(yaml.victim().name(), "victim.name", violations);
        requireText(yaml.victim().deathLocationCode(), "victim.deathLocationCode", violations);
        if (hasText(yaml.victim().deathLocationCode()) && !locationCodes.contains(yaml.victim().deathLocationCode())) {
            violations.add("victim.deathLocationCode references missing location: " + yaml.victim().deathLocationCode());
        }
    }

    private void validateLocations(ScenarioYaml yaml, List<String> violations) {
        for (ScenarioYaml.LocationYaml location : listOf(yaml.locations())) {
            if (location == null) {
                violations.add("locations[] item is required.");
                continue;
            }
            requireText(location.code(), "locations[].code", violations);
            requireText(location.name(), "locations[" + location.code() + "].name", violations);
            validateAssetKey(location.imageAssetKey(), "locations[" + location.code() + "].imageAssetKey", violations);
            if (isPublished(yaml)) {
                requireNumber(location.mapX(), "locations[" + location.code() + "].mapX", violations);
                requireNumber(location.mapY(), "locations[" + location.code() + "].mapY", violations);
            }
        }
    }

    private void validateMetadata(ScenarioYaml yaml, List<String> violations) {
        if (yaml.metadata() == null) {
            violations.add("metadata section is required.");
            return;
        }
        requireText(yaml.metadata().contentStatus(), "metadata.contentStatus", violations);
        requireText(yaml.metadata().locale(), "metadata.locale", violations);
    }

    private void validatePublishedRootSections(ScenarioYaml yaml, List<String> violations) {
        if (!isPublished(yaml)) {
            return;
        }
        if (yaml.metadata() == null) {
            violations.add("PUBLISHED requires metadata section.");
        }
        if (yaml.scenario() == null) {
            violations.add("PUBLISHED requires scenario section.");
        }
        if (yaml.victim() == null) {
            violations.add("PUBLISHED requires victim section.");
        }
        requireNonEmpty(yaml.locations(), "locations", violations);
        requireNonEmpty(yaml.characters(), "characters", violations);
        requireNonEmpty(yaml.evidences(), "evidences", violations);
        requireNonEmpty(yaml.variants(), "variants", violations);
        requireNonEmpty(yaml.unlockRules(), "unlockRules", violations);
        requireNonEmpty(yaml.npcPolicies(), "npcPolicies", violations);
        if (yaml.scoring() == null || yaml.scoring().isEmpty()) {
            violations.add("PUBLISHED requires non-empty scoring.");
        }
        requireNonEmpty(yaml.assets(), "assets", violations);
    }

    private void validateEvidenceReferences(ScenarioYaml yaml,
                                            Set<String> locationCodes,
                                            Set<String> characterCodes,
                                            List<String> violations) {
        for (ScenarioYaml.EvidenceYaml evidence : listOf(yaml.evidences())) {
            requireText(evidence.code(), "evidences[].code", violations);
            requireText(evidence.title(), "evidences[" + evidence.code() + "].title", violations);
            requireText(evidence.category(), "evidences[" + evidence.code() + "].category", violations);
            requireText(evidence.unlockPhase(), "evidences[" + evidence.code() + "].unlockPhase", violations);
            if (hasText(evidence.locationCode()) && !locationCodes.contains(evidence.locationCode())) {
                violations.add("evidence " + evidence.code() + " references missing location: " + evidence.locationCode());
            }
            for (String characterCode : listOf(evidence.relatedCharacterCodes())) {
                if (!characterCodes.contains(characterCode)) {
                    violations.add("evidence " + evidence.code()
                            + " relatedCharacterCodes references missing character: " + characterCode);
                }
            }
            validateAssetKey(evidence.imageAssetKey(), "evidences[" + evidence.code() + "].imageAssetKey", violations);
            validateAssetKey(evidence.thumbnailAssetKey(), "evidences[" + evidence.code() + "].thumbnailAssetKey", violations);
        }
    }

    private void validateVariantReferences(ScenarioYaml yaml,
                                           Map<String, ScenarioYaml.CharacterYaml> charactersByCode,
                                           Set<String> evidenceCodes,
                                           List<String> violations) {
        for (ScenarioYaml.VariantYaml variant : listOf(yaml.variants())) {
            requireText(variant.code(), "variants[].code", violations);
            requireText(variant.culpritCode(), "variants[" + variant.code() + "].culpritCode", violations);
            ScenarioYaml.CharacterYaml culprit = charactersByCode.get(variant.culpritCode());
            if (culprit == null) {
                violations.add("variant " + variant.code() + " references missing culprit character: " + variant.culpritCode());
            } else if (!Boolean.TRUE.equals(culprit.culpritEligible())) {
                violations.add("variant " + variant.code() + " uses non-culprit-eligible character: " + variant.culpritCode());
            }

            for (String evidenceCode : collectEvidenceCodes(variant.solution())) {
                if (!evidenceCodes.contains(evidenceCode)) {
                    violations.add("variant " + variant.code() + " solution references missing evidence: " + evidenceCode);
                }
            }
            for (String evidenceCode : listOf(variant.misleadingEvidenceCodes())) {
                if (!evidenceCodes.contains(evidenceCode)) {
                    violations.add("variant " + variant.code() + " misleadingEvidenceCodes references missing evidence: " + evidenceCode);
                }
            }
        }
    }

    private void validateTimelineEvents(ScenarioYaml yaml,
                                        Set<String> locationCodes,
                                        Set<String> evidenceCodes,
                                        Set<String> characterCodes,
                                        List<String> violations) {
        Set<String> eventCodes = new HashSet<>();
        Set<Integer> eventOrders = new HashSet<>();
        for (ScenarioYaml.TimelineEventYaml event : listOf(yaml.timelineEvents())) {
            if (event == null) {
                violations.add("timelineEvents[] item is required.");
                continue;
            }

            requireText(event.code(), "timelineEvents[].code", violations);
            if (hasText(event.code()) && !eventCodes.add(event.code())) {
                violations.add("duplicate timelineEvent code: " + event.code());
            }

            requireNumber(event.eventOrder(), "timelineEvents[" + event.code() + "].eventOrder", violations);
            if (event.eventOrder() != null && !eventOrders.add(event.eventOrder())) {
                violations.add("duplicate timelineEvent eventOrder: " + event.eventOrder());
            }

            requireText(event.eventTime(), "timelineEvents[" + event.code() + "].eventTime", violations);
            requireText(event.title(), "timelineEvents[" + event.code() + "].title", violations);
            requireText(event.eventType(), "timelineEvents[" + event.code() + "].eventType", violations);
            requireText(event.visibility(), "timelineEvents[" + event.code() + "].visibility", violations);
            validateTimelineVisibility(event, violations);
            if (event.isTrueEvent() == null) {
                violations.add("timelineEvents[" + event.code() + "].isTrueEvent is required.");
            }

            if (hasText(event.locationCode()) && !locationCodes.contains(event.locationCode())) {
                violations.add("timelineEvent " + event.code()
                        + " references missing location: " + event.locationCode());
            }
            if (hasText(event.relatedEvidenceCode()) && !evidenceCodes.contains(event.relatedEvidenceCode())) {
                violations.add("timelineEvent " + event.code()
                        + " references missing related evidence: " + event.relatedEvidenceCode());
            }
            if (hasText(event.relatedCharacterCode()) && !characterCodes.contains(event.relatedCharacterCode())) {
                violations.add("timelineEvent " + event.code()
                        + " references missing related character: " + event.relatedCharacterCode());
            }
        }
    }

    private void validateTimelineVisibility(ScenarioYaml.TimelineEventYaml event, List<String> violations) {
        if (!hasText(event.visibility())) {
            return;
        }
        String visibility = event.visibility();
        if (!TIMELINE_EVENT_VISIBILITIES.contains(visibility)) {
            violations.add("timelineEvents[" + event.code() + "].visibility must be one of "
                    + TIMELINE_EVENT_VISIBILITIES + ": " + visibility);
        }
    }

    private void validatePublishedVariants(ScenarioYaml yaml, List<String> violations) {
        if (!isPublished(yaml)) {
            return;
        }

        boolean hasEnabledVariant = listOf(yaml.variants()).stream()
                .anyMatch(variant -> Boolean.TRUE.equals(variant.enabled()));
        if (!hasEnabledVariant) {
            violations.add("PUBLISHED requires at least one enabled variant.");
        }

        for (ScenarioYaml.VariantYaml variant : listOf(yaml.variants())) {
            if (!Boolean.TRUE.equals(variant.enabled())) {
                continue;
            }

            Map<String, Object> solution = variant.solution();
            if (solution == null || solution.isEmpty()) {
                violations.add("enabled variant " + variant.code() + " requires non-empty solution.");
                continue;
            }

            requireObjectText(solution.get("motiveSummary"),
                    "variants[" + variant.code() + "].solution.motiveSummary", violations);
            requireObjectText(solution.get("methodSummary"),
                    "variants[" + variant.code() + "].solution.methodSummary", violations);
            requireObjectText(solution.get("coverUpSummary"),
                    "variants[" + variant.code() + "].solution.coverUpSummary", violations);
            requireObjectText(solution.get("solutionText"),
                    "variants[" + variant.code() + "].solution.solutionText", violations);

            Object proofDimensions = solution.get("proofDimensions");
            if (!(proofDimensions instanceof Map<?, ?> proofDimensionMap) || proofDimensionMap.isEmpty()) {
                violations.add("variants[" + variant.code() + "].solution.proofDimensions is required.");
            }
        }
    }

    private void validateEvidenceVariantStates(ScenarioYaml yaml,
                                               Set<String> evidenceCodes,
                                               Set<String> variantCodes,
                                               List<String> violations) {
        Set<String> stateKeys = new HashSet<>();
        for (ScenarioYaml.EvidenceVariantStateYaml state : listOf(yaml.evidenceVariantStates())) {
            if (!variantCodes.contains(state.variantCode())) {
                violations.add("evidenceVariantState references missing variant: " + state.variantCode());
            }
            if (!evidenceCodes.contains(state.evidenceCode())) {
                violations.add("evidenceVariantState references missing evidence: " + state.evidenceCode());
            }
            String stateKey = state.variantCode() + "::" + state.evidenceCode();
            if (!stateKeys.add(stateKey)) {
                violations.add("duplicate evidenceVariantState: " + stateKey);
            }
        }
    }

    private void validateUnlockRules(ScenarioYaml yaml,
                                     Set<String> evidenceCodes,
                                     Set<String> characterCodes,
                                     List<String> violations) {
        Set<String> ruleEvidenceCodes = new HashSet<>();
        for (ScenarioYaml.UnlockRuleYaml rule : listOf(yaml.unlockRules())) {
            if (!evidenceCodes.contains(rule.evidenceCode())) {
                violations.add("unlockRule references missing evidence: " + rule.evidenceCode());
            } else if (!ruleEvidenceCodes.add(rule.evidenceCode())) {
                violations.add("duplicate unlockRule for evidence: " + rule.evidenceCode());
            }

            ScenarioYaml.UnlockConditionYaml condition = rule.condition();
            if (condition == null) {
                violations.add("unlockRule " + rule.evidenceCode() + " condition is required.");
                continue;
            }
            for (String requiredEvidenceCode : listOf(condition.requiredEvidenceCodes())) {
                if (!evidenceCodes.contains(requiredEvidenceCode)) {
                    violations.add("unlockRule " + rule.evidenceCode() + " references missing required evidence: " + requiredEvidenceCode);
                }
            }
            if (hasText(condition.requiredCharacterCode()) && !characterCodes.contains(condition.requiredCharacterCode())) {
                violations.add("unlockRule " + rule.evidenceCode() + " references missing character: " + condition.requiredCharacterCode());
            }
            if (hasText(condition.requiredPresentedEvidenceCode())
                    && !evidenceCodes.contains(condition.requiredPresentedEvidenceCode())) {
                violations.add("unlockRule " + rule.evidenceCode()
                        + " references missing presented evidence: " + condition.requiredPresentedEvidenceCode());
            }
            // EVIDENCE_PRESENTED 해금은 트리거 증거가 반드시 있어야 한다.
            // (runtime matcher가 requiredPresentedEvidenceCode를 필수로 보므로, 비면 import는 통과해도 런타임에서 영원히 매칭 실패한다.)
            if ("EVIDENCE_PRESENTED".equalsIgnoreCase(rule.unlockType())
                    && !hasText(condition.requiredPresentedEvidenceCode())) {
                violations.add("unlockRule " + rule.evidenceCode()
                        + " is EVIDENCE_PRESENTED but condition.requiredPresentedEvidenceCode is missing.");
            }
            // 자기 자신을 트리거로 지정하면 모순(잠긴 증거를 그 자신 제시로 여는 셈)이므로 차단한다.
            if ("EVIDENCE_PRESENTED".equalsIgnoreCase(rule.unlockType())
                    && hasText(condition.requiredPresentedEvidenceCode())
                    && condition.requiredPresentedEvidenceCode().equals(rule.evidenceCode())) {
                violations.add("unlockRule " + rule.evidenceCode()
                        + " is EVIDENCE_PRESENTED but requiredPresentedEvidenceCode references itself.");
            }
        }
    }

    private void validateNpcPolicies(ScenarioYaml yaml,
                                     Set<String> evidenceCodes,
                                     Set<String> characterCodes,
                                     List<String> violations) {
        Set<String> policyCharacterCodes = new HashSet<>();
        for (ScenarioYaml.NpcPolicyYaml policy : listOf(yaml.npcPolicies())) {
            if (!characterCodes.contains(policy.characterCode())) {
                violations.add("npcPolicy references missing character: " + policy.characterCode());
            } else if (!policyCharacterCodes.add(policy.characterCode())) {
                violations.add("duplicate npcPolicy for character: " + policy.characterCode());
            }

            for (ScenarioYaml.NpcEvidenceReactionPolicyYaml reaction : listOf(policy.evidenceReactionPolicies())) {
                if (!evidenceCodes.contains(reaction.evidenceCode())) {
                    violations.add("npcPolicy " + policy.characterCode() + " references missing reaction evidence: " + reaction.evidenceCode());
                }
            }
        }
    }

    private void validateAssets(ScenarioYaml yaml,
                                String scenarioCode,
                                String victimCode,
                                Set<String> locationCodes,
                                Set<String> characterCodes,
                                Set<String> evidenceCodes,
                                Set<String> variantCodes,
                                List<String> violations) {
        Set<String> assetKeys = new HashSet<>();
        for (ScenarioYaml.AssetYaml asset : listOf(yaml.assets())) {
            requireText(asset.assetKey(), "assets[].assetKey", violations);
            requireText(asset.type(), "assets[" + asset.assetKey() + "].type", violations);
            requireText(asset.targetKind(), "assets[" + asset.assetKey() + "].targetKind", violations);
            requireText(asset.targetCode(), "assets[" + asset.assetKey() + "].targetCode", violations);
            if (hasText(asset.assetKey()) && !assetKeys.add(asset.assetKey())) {
                violations.add("duplicate assetKey: " + asset.assetKey());
            }
            validateAssetKey(asset.assetKey(), "assets[].assetKey", violations);
            validateAssetKey(asset.s3ObjectKey(), "assets[" + asset.assetKey() + "].s3ObjectKey", violations);
            validateAssetTarget(asset, scenarioCode, victimCode, locationCodes, characterCodes,
                    evidenceCodes, variantCodes, violations);
            if (hasText(asset.sourceLocalFile()) && LOCAL_PATH_PATTERN.matcher(asset.sourceLocalFile()).matches()) {
                violations.add("asset sourceLocalFile must be relative, not a local absolute path: " + asset.sourceLocalFile());
            }
        }
    }

    private void validateAssetTarget(ScenarioYaml.AssetYaml asset,
                                     String scenarioCode,
                                     String victimCode,
                                     Set<String> locationCodes,
                                     Set<String> characterCodes,
                                     Set<String> evidenceCodes,
                                     Set<String> variantCodes,
                                     List<String> violations) {
        if (!hasText(asset.targetKind()) || !hasText(asset.targetCode())) {
            return;
        }

        String targetKind = asset.targetKind().toUpperCase();
        boolean exists = switch (targetKind) {
            case "SCENARIO" -> Objects.equals(asset.targetCode(), scenarioCode);
            case "VICTIM" -> Objects.equals(asset.targetCode(), victimCode);
            case "LOCATION" -> locationCodes.contains(asset.targetCode());
            case "CHARACTER", "SUSPECT", "WITNESS", "NPC" -> characterCodes.contains(asset.targetCode());
            case "EVIDENCE" -> evidenceCodes.contains(asset.targetCode());
            case "VARIANT" -> variantCodes.contains(asset.targetCode());
            default -> {
                violations.add("asset " + asset.assetKey() + " has unsupported targetKind: " + asset.targetKind());
                yield true;
            }
        };
        if (!exists) {
            violations.add("asset " + asset.assetKey() + " targetCode references missing "
                    + asset.targetKind() + ": " + asset.targetCode());
        }
    }

    private <T> Set<String> collectCodes(List<T> items,
                                         Function<T, String> codeExtractor,
                                         String section,
                                         List<String> violations) {
        return collectCodeMap(items, codeExtractor, section, violations).keySet();
    }

    private <T> Map<String, T> collectCodeMap(List<T> items,
                                              Function<T, String> codeExtractor,
                                              String section,
                                              List<String> violations) {
        Map<String, List<T>> grouped = listOf(items).stream()
                .filter(Objects::nonNull)
                .filter(item -> hasText(codeExtractor.apply(item)))
                .collect(Collectors.groupingBy(codeExtractor));

        grouped.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .forEach(entry -> violations.add("duplicate code in " + section + ": " + entry.getKey()));

        return grouped.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getFirst()));
    }

    private Set<String> collectEvidenceCodes(Object value) {
        Set<String> codes = new HashSet<>();
        collectEvidenceCodes(value, codes);
        return codes;
    }

    private void collectEvidenceCodes(Object value, Set<String> codes) {
        if (value instanceof String stringValue) {
            if (stringValue.startsWith("EVIDENCE_")) {
                codes.add(stringValue);
            }
            return;
        }
        if (value instanceof Map<?, ?> mapValue) {
            mapValue.values().forEach(item -> collectEvidenceCodes(item, codes));
            return;
        }
        if (value instanceof Collection<?> collectionValue) {
            collectionValue.forEach(item -> collectEvidenceCodes(item, codes));
        }
    }

    private void validateAssetKey(String value, String field, List<String> violations) {
        if (!hasText(value)) {
            return;
        }
        if (LOCAL_PATH_PATTERN.matcher(value).matches()) {
            violations.add(field + " must not contain a local filesystem path: " + value);
        }
        if (!ASSET_KEY_PATTERN.matcher(value).matches()) {
            violations.add(field + " has unsupported characters: " + value);
        }
    }

    private void requireText(String value, String field, List<String> violations) {
        if (!hasText(value)) {
            violations.add(field + " is required.");
        }
    }

    private void requireObjectText(Object value, String field, List<String> violations) {
        if (value == null || String.valueOf(value).isBlank()) {
            violations.add(field + " is required.");
        }
    }

    private void requireNumber(Integer value, String field, List<String> violations) {
        if (value == null) {
            violations.add(field + " is required.");
        }
    }

    private <T> void requireNonEmpty(List<T> value, String section, List<String> violations) {
        if (value == null || value.isEmpty()) {
            violations.add("PUBLISHED requires non-empty " + section + ".");
        }
    }

    private boolean isPublished(ScenarioYaml yaml) {
        return isPublishedValue(yaml.metadata() == null ? null : yaml.metadata().contentStatus())
                || isPublishedValue(yaml.scenario() == null ? null : yaml.scenario().status());
    }

    private boolean isPublishedValue(String value) {
        return hasText(value) && "PUBLISHED".equalsIgnoreCase(value);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private <T> List<T> listOf(List<T> source) {
        return source == null ? List.of() : source;
    }
}
