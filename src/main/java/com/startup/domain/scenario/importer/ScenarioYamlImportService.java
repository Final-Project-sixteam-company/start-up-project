package com.startup.domain.scenario.importer;

import com.startup.domain.ai.entity.SuspectResponsePolicy;
import com.startup.domain.ai.repository.SuspectResponsePolicyRepository;
import com.startup.domain.scenario.entity.Evidence;
import com.startup.domain.scenario.entity.EvidenceSuspect;
import com.startup.domain.scenario.entity.EvidenceUnlockRule;
import com.startup.domain.scenario.entity.EvidenceVariantState;
import com.startup.domain.scenario.entity.Hint;
import com.startup.domain.scenario.entity.NpcKnowledgeProfile;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.entity.ScenarioAsset;
import com.startup.domain.scenario.entity.ScenarioLocation;
import com.startup.domain.scenario.entity.ScenarioVariant;
import com.startup.domain.scenario.entity.Suspect;
import com.startup.domain.scenario.entity.TimelineEvent;
import com.startup.domain.scenario.entity.VariantSolution;
import com.startup.domain.scenario.entity.Victim;
import com.startup.domain.scenario.enums.Difficulty;
import com.startup.domain.scenario.enums.EvidenceImportance;
import com.startup.domain.scenario.enums.EvidenceType;
import com.startup.domain.scenario.enums.EvidenceUnlockType;
import com.startup.domain.scenario.enums.RelationType;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioType;
import com.startup.domain.scenario.enums.ScenarioVisibility;
import com.startup.domain.scenario.enums.VariantType;
import com.startup.domain.scenario.importer.yaml.ScenarioYaml;
import com.startup.domain.scenario.repository.EvidenceRepository;
import com.startup.domain.scenario.repository.EvidenceSuspectRepository;
import com.startup.domain.scenario.repository.EvidenceUnlockRuleRepository;
import com.startup.domain.scenario.repository.EvidenceVariantStateRepository;
import com.startup.domain.scenario.repository.HintRepository;
import com.startup.domain.scenario.repository.NpcKnowledgeProfileRepository;
import com.startup.domain.scenario.repository.ScenarioAssetRepository;
import com.startup.domain.scenario.repository.ScenarioLocationRepository;
import com.startup.domain.scenario.repository.ScenarioRepository;
import com.startup.domain.scenario.repository.ScenarioVariantRepository;
import com.startup.domain.scenario.repository.SuspectRepository;
import com.startup.domain.scenario.repository.TimelineEventRepository;
import com.startup.domain.scenario.repository.VariantSolutionRepository;
import com.startup.domain.scenario.repository.VictimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScenarioYamlImportService {

    private final ScenarioYamlValidator validator;
    private final ScenarioRepository scenarioRepository;
    private final ScenarioLocationRepository locationRepository;
    private final VictimRepository victimRepository;
    private final SuspectRepository suspectRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvidenceSuspectRepository evidenceSuspectRepository;
    private final ScenarioVariantRepository variantRepository;
    private final VariantSolutionRepository solutionRepository;
    private final EvidenceVariantStateRepository evidenceVariantStateRepository;
    private final EvidenceUnlockRuleRepository evidenceUnlockRuleRepository;
    private final NpcKnowledgeProfileRepository npcKnowledgeProfileRepository;
    private final ScenarioAssetRepository scenarioAssetRepository;
    private final SuspectResponsePolicyRepository suspectResponsePolicyRepository;
    private final TimelineEventRepository timelineEventRepository;
    private final HintRepository hintRepository;
    private final JsonMapper jsonMapper;

    @Transactional
    public ScenarioImportResult importYaml(ScenarioYaml yaml, String contentHash, String sourceName) {
        Scenario existing = findExistingScenarioIfAddressable(yaml);
        if (existing != null) {
            String scenarioCode = yaml.scenario().code();
            String version = yaml.scenario().version();
            if (Objects.equals(existing.getContentHash(), contentHash)) {
                // Existing production seeds must stay restart-safe when new validators become stricter.
                return ScenarioImportResult.skipped(existing.getId(), scenarioCode, version);
            }
            throw new ScenarioImportException("같은 scenario.code/version의 YAML 내용이 변경되었습니다. "
                    + "version을 올린 뒤 다시 import하세요: " + scenarioCode + "@" + version
                    + " source=" + sourceName);
        }

        validator.validateOrThrow(yaml);

        String scenarioCode = yaml.scenario().code();
        String version = yaml.scenario().version();
        Map<String, ScenarioYaml.UnlockRuleYaml> unlockRulesByEvidenceCode = mapByCode(
                listOf(yaml.unlockRules()), ScenarioYaml.UnlockRuleYaml::evidenceCode);

        Scenario scenario = saveScenario(yaml, contentHash);
        Map<String, ScenarioLocation> locationsByCode = saveLocations(yaml, scenario.getId());
        saveVictim(yaml, scenario.getId(), locationsByCode);
        Map<String, Suspect> suspectsByCode = saveCharacters(yaml, scenario.getId());
        Map<String, Evidence> evidencesByCode = saveEvidences(
                yaml, scenario.getId(), locationsByCode, unlockRulesByEvidenceCode);
        saveEvidenceSuspects(yaml, suspectsByCode, evidencesByCode);
        saveTimelineEvents(yaml, scenario.getId(), suspectsByCode, evidencesByCode);
        Map<String, ScenarioVariant> variantsByCode = saveVariantsAndSolutions(
                yaml, scenario.getId(), suspectsByCode, evidencesByCode);
        saveEvidenceVariantStates(yaml, scenario.getId(), variantsByCode, evidencesByCode);
        saveHints(yaml, scenario.getId());
        saveUnlockRules(yaml, scenario.getId(), evidencesByCode, unlockRulesByEvidenceCode);
        saveNpcPolicies(yaml, scenario.getId(), suspectsByCode, evidencesByCode);
        saveAssets(yaml, scenario.getId());

        return new ScenarioImportResult(
                ScenarioImportResult.Status.IMPORTED,
                scenario.getId(),
                scenarioCode,
                version,
                listOf(yaml.locations()).size(),
                listOf(yaml.characters()).size(),
                listOf(yaml.evidences()).size(),
                listOf(yaml.variants()).size(),
                listOf(yaml.evidenceVariantStates()).size(),
                listOf(yaml.hints()).size(),
                listOf(yaml.unlockRules()).size(),
                listOf(yaml.npcPolicies()).size(),
                listOf(yaml.timelineEvents()).size(),
                listOf(yaml.assets()).size()
        );
    }

    private Scenario findExistingScenarioIfAddressable(ScenarioYaml yaml) {
        if (yaml == null || yaml.scenario() == null) {
            return null;
        }
        String scenarioCode = yaml.scenario().code();
        String version = yaml.scenario().version();
        if (!hasText(scenarioCode) || !hasText(version)) {
            return null;
        }
        return scenarioRepository.findByCodeAndVersion(scenarioCode, version).orElse(null);
    }

    private Scenario saveScenario(ScenarioYaml yaml, String contentHash) {
        ScenarioYaml.ScenarioSectionYaml scenario = yaml.scenario();
        return scenarioRepository.save(Scenario.builder()
                .code(scenario.code())
                .version(scenario.version())
                .contentHash(contentHash)
                .title(scenario.title())
                .description(scenario.description())
                .synopsis(scenario.synopsis())
                .genre(scenario.genre())
                .scenarioType(enumValue(ScenarioType.class, scenario.scenarioType(), "scenario.scenarioType"))
                .visibility(enumValue(ScenarioVisibility.class, scenario.visibility(), "scenario.visibility"))
                .difficulty(enumValue(Difficulty.class, scenario.difficulty(), "scenario.difficulty"))
                .estimatedPlayTimeMinutes(scenario.estimatedPlayTimeMinutes())
                .playerCountMin(1)
                .playerCountMax(1)
                .priceCredit(0)
                .creatorId(null)
                .culpritMode(scenario.culpritMode())
                .deductionMode(scenario.deductionMode())
                .mapMode(scenario.mapMode())
                .evidenceMode(scenario.evidenceMode())
                .coverAssetKey(scenario.coverAssetKey())
                .mapAssetKey(scenario.mapAssetKey())
                .status(enumValue(ScenarioStatus.class, scenario.status(), "scenario.status"))
                .build());
    }

    private Map<String, ScenarioLocation> saveLocations(ScenarioYaml yaml, Long scenarioId) {
        Map<String, ScenarioLocation> result = new LinkedHashMap<>();
        for (ScenarioYaml.LocationYaml locationYaml : listOf(yaml.locations())) {
            ScenarioLocation location = locationRepository.save(ScenarioLocation.builder()
                    .scenarioId(scenarioId)
                    .code(locationYaml.code())
                    .name(locationYaml.name())
                    .floor(locationYaml.floor())
                    .description(locationYaml.description())
                    .imageAssetKey(locationYaml.imageAssetKey())
                    .mapX(locationYaml.mapX())
                    .mapY(locationYaml.mapY())
                    .sortOrder(locationYaml.sortOrder())
                    .build());
            result.put(locationYaml.code(), location);
        }
        return result;
    }

    private void saveVictim(ScenarioYaml yaml,
                            Long scenarioId,
                            Map<String, ScenarioLocation> locationsByCode) {
        ScenarioYaml.VictimYaml victimYaml = yaml.victim();
        ScenarioLocation foundLocation = locationsByCode.get(victimYaml.deathLocationCode());
        victimRepository.save(Victim.builder()
                .scenarioId(scenarioId)
                .foundLocationId(foundLocation == null ? null : foundLocation.getId())
                .name(victimYaml.name())
                .role(victimYaml.roleLabel())
                .description(victimYaml.publicProfile())
                .causeOfDeath(victimYaml.publicCauseOfDeathText())
                .foundCondition(victimYaml.discoveredAtText())
                .build());
    }

    private Map<String, Suspect> saveCharacters(ScenarioYaml yaml, Long scenarioId) {
        Map<String, Suspect> result = new LinkedHashMap<>();
        for (ScenarioYaml.CharacterYaml characterYaml : listOf(yaml.characters())) {
            Suspect suspect = suspectRepository.save(Suspect.builder()
                    .scenarioId(scenarioId)
                    .code(characterYaml.code())
                    .name(characterYaml.name())
                    .role(characterYaml.roleLabel())
                    .characterType(characterYaml.characterType())
                    .culpritEligible(characterYaml.culpritEligible())
                    .relationToVictim(null)
                    .publicProfile(characterYaml.publicProfile())
                    .publicStatement(characterYaml.publicProfile())
                    .alibi(characterYaml.publicAlibi())
                    .personalityPrompt(characterYaml.personalityTone())
                    .responsePolicyJson(null)
                    .portraitAssetKey(characterYaml.portraitAssetKey())
                    .suspicionLevel(Boolean.TRUE.equals(characterYaml.culpritEligible()) ? 50 : 15)
                    .sortOrder(characterYaml.sortOrder())
                    .build());
            result.put(characterYaml.code(), suspect);
        }
        return result;
    }

    private Map<String, Evidence> saveEvidences(ScenarioYaml yaml,
                                                Long scenarioId,
                                                Map<String, ScenarioLocation> locationsByCode,
                                                Map<String, ScenarioYaml.UnlockRuleYaml> unlockRulesByEvidenceCode) {
        Map<String, Evidence> result = new LinkedHashMap<>();
        for (ScenarioYaml.EvidenceYaml evidenceYaml : listOf(yaml.evidences())) {
            ScenarioLocation location = locationsByCode.get(evidenceYaml.locationCode());
            ScenarioYaml.UnlockRuleYaml unlockRule = unlockRulesByEvidenceCode.get(evidenceYaml.code());
            Evidence evidence = evidenceRepository.save(Evidence.builder()
                    .scenarioId(scenarioId)
                    .code(evidenceYaml.code())
                    .locationId(location == null ? null : location.getId())
                    .title(evidenceYaml.title())
                    .description(evidenceYaml.baseDetail())
                    .oneLine(evidenceYaml.oneLine())
                    .evidenceType(enumValue(EvidenceType.class, evidenceYaml.category(), "evidences[].category"))
                    .importance(toImportance(evidenceYaml.baseRole()))
                    .imageAssetKey(evidenceYaml.imageAssetKey())
                    .thumbnailAssetKey(evidenceYaml.thumbnailAssetKey())
                    .tagsJson(toJson(evidenceYaml.tags()))
                    .guidanceJson(toNullableJson(evidenceYaml.guidance()))
                    .unlockPhase(evidenceYaml.unlockPhase())
                    .isInitialPublic(isOpeningPhase(evidenceYaml.unlockPhase()))
                    .unlockType(toUnlockType(unlockRule))
                    .unlockConditionJson(toConditionJson(evidenceYaml, unlockRule))
                    .sortOrder(evidenceYaml.sortOrder())
                    .build());
            result.put(evidenceYaml.code(), evidence);
        }
        return result;
    }

    private void saveEvidenceSuspects(ScenarioYaml yaml,
                                      Map<String, Suspect> suspectsByCode,
                                      Map<String, Evidence> evidencesByCode) {
        for (ScenarioYaml.EvidenceYaml evidenceYaml : listOf(yaml.evidences())) {
            Evidence evidence = evidencesByCode.get(evidenceYaml.code());
            for (String characterCode : listOf(evidenceYaml.relatedCharacterCodes())) {
                Suspect suspect = suspectsByCode.get(characterCode);
                if (evidence == null || suspect == null) {
                    throw new ScenarioImportException("evidence relatedCharacterCodes reference not found: "
                            + evidenceYaml.code() + "/" + characterCode);
                }
                evidenceSuspectRepository.save(EvidenceSuspect.builder()
                        .evidenceId(evidence.getId())
                        .suspectId(suspect.getId())
                        .relationType(RelationType.RELATED)
                        .build());
            }
        }
    }

    private void saveTimelineEvents(ScenarioYaml yaml,
                                    Long scenarioId,
                                    Map<String, Suspect> suspectsByCode,
                                    Map<String, Evidence> evidencesByCode) {
        for (ScenarioYaml.TimelineEventYaml eventYaml : listOf(yaml.timelineEvents())) {
            Suspect relatedSuspect = hasText(eventYaml.relatedCharacterCode())
                    ? suspectsByCode.get(eventYaml.relatedCharacterCode())
                    : null;
            Evidence relatedEvidence = hasText(eventYaml.relatedEvidenceCode())
                    ? evidencesByCode.get(eventYaml.relatedEvidenceCode())
                    : null;

            timelineEventRepository.save(TimelineEvent.builder()
                    .scenarioId(scenarioId)
                    .relatedSuspectId(relatedSuspect == null ? null : relatedSuspect.getId())
                    .relatedEvidenceId(relatedEvidence == null ? null : relatedEvidence.getId())
                    .eventTime(eventYaml.eventTime())
                    .eventOrder(eventYaml.eventOrder())
                    .title(eventYaml.title())
                    .description(eventYaml.description())
                    .eventType(eventYaml.eventType())
                    .isTrueEvent(Boolean.TRUE.equals(eventYaml.isTrueEvent()))
                    .visibility(eventYaml.visibility())
                    .build());
        }
    }

    private Map<String, ScenarioVariant> saveVariantsAndSolutions(ScenarioYaml yaml,
                                                                  Long scenarioId,
                                                                  Map<String, Suspect> suspectsByCode,
                                                                  Map<String, Evidence> evidencesByCode) {
        Map<String, ScenarioVariant> result = new LinkedHashMap<>();
        for (ScenarioYaml.VariantYaml variantYaml : listOf(yaml.variants())) {
            Suspect culprit = suspectsByCode.get(variantYaml.culpritCode());
            if (culprit == null) {
                throw new ScenarioImportException("variant culprit not found: " + variantYaml.culpritCode());
            }

            Map<String, Object> solution = mapOf(variantYaml.solution());
            String method = firstNonBlank(stringValue(solution.get("methodSummary")),
                    variantYaml.mainMethodLayer(), variantYaml.fatalLayerSummary(), variantYaml.directCause());
            ScenarioVariant variant = variantRepository.save(ScenarioVariant.builder()
                    .scenarioId(scenarioId)
                    .code(variantYaml.code())
                    .variantType(toVariantType(variantYaml.code()))
                    .variantName(culprit.getName() + " Variant")
                    .description(firstNonBlank(variantYaml.mainMethodLayer(),
                            variantYaml.fatalLayerSummary(), variantYaml.directCause(), method))
                    .culpritCode(variantYaml.culpritCode())
                    .weight(variantYaml.weight())
                    .isActive(Boolean.TRUE.equals(variantYaml.enabled()))
                    .sortOrder(result.size() + 1)
                    .build());
            result.put(variantYaml.code(), variant);

            solutionRepository.save(VariantSolution.builder()
                    .variantId(variant.getId())
                    .culpritSuspectId(culprit.getId())
                    .culpritCode(variantYaml.culpritCode())
                    .culpritName(culprit.getName())
                    .culpritRole(culprit.getRole())
                    .motive(stringValue(solution.get("motiveSummary")))
                    .method(method)
                    .coverUp(stringValue(solution.get("coverUpSummary")))
                    .fullExplanation(stringValue(solution.get("solutionText")))
                    .keyEvidenceIds(toEvidenceIdCsv(collectEvidenceCodes(solution), evidencesByCode))
                    .proofDimensionJson(toJson(solution.get("proofDimensions")))
                    .finalFeedbackJson(toJson(Map.of(
                            "solution", solution,
                            "misleadingEvidenceCodes", listOf(variantYaml.misleadingEvidenceCodes())
                    )))
                    .build());
        }
        return result;
    }

    private void saveEvidenceVariantStates(ScenarioYaml yaml,
                                           Long scenarioId,
                                           Map<String, ScenarioVariant> variantsByCode,
                                           Map<String, Evidence> evidencesByCode) {
        for (ScenarioYaml.EvidenceVariantStateYaml stateYaml : listOf(yaml.evidenceVariantStates())) {
            ScenarioVariant variant = variantsByCode.get(stateYaml.variantCode());
            Evidence evidence = evidencesByCode.get(stateYaml.evidenceCode());
            if (variant == null || evidence == null) {
                throw new ScenarioImportException("evidenceVariantState reference not found: "
                        + stateYaml.variantCode() + "/" + stateYaml.evidenceCode());
            }
            evidenceVariantStateRepository.save(EvidenceVariantState.builder()
                    .scenarioId(scenarioId)
                    .variantId(variant.getId())
                    .evidenceId(evidence.getId())
                    .variantCode(stateYaml.variantCode())
                    .evidenceCode(stateYaml.evidenceCode())
                    .role(stateYaml.role())
                    .detailOverride(stateYaml.detailOverride())
                    .detailAppend(stateYaml.detailAppend())
                    .proofDimensionsJson(toJson(stateYaml.proofDimensions()))
                    .build());
        }
    }

    private void saveHints(ScenarioYaml yaml, Long scenarioId) {
        for (ScenarioYaml.HintYaml hintYaml : listOf(yaml.hints())) {
            hintRepository.save(Hint.builder()
                    .scenarioId(scenarioId)
                    .hintLevel(hintYaml.hintLevel())
                    .content(hintYaml.content())
                    .unlockAfterMinutes(hintYaml.unlockAfterMinutes())
                    .penaltyScore(hintYaml.penaltyScore())
                    .build());
        }
    }

    private void saveUnlockRules(ScenarioYaml yaml,
                                 Long scenarioId,
                                 Map<String, Evidence> evidencesByCode,
                                 Map<String, ScenarioYaml.UnlockRuleYaml> unlockRulesByEvidenceCode) {
        for (ScenarioYaml.UnlockRuleYaml ruleYaml : listOf(yaml.unlockRules())) {
            Evidence evidence = evidencesByCode.get(ruleYaml.evidenceCode());
            if (evidence == null) {
                throw new ScenarioImportException("unlockRule evidence not found: " + ruleYaml.evidenceCode());
            }
            evidenceUnlockRuleRepository.save(EvidenceUnlockRule.builder()
                    .scenarioId(scenarioId)
                    .evidenceId(evidence.getId())
                    .evidenceCode(ruleYaml.evidenceCode())
                    .unlockType(ruleYaml.unlockType())
                    .requiredPhase(ruleYaml.condition() == null ? null : ruleYaml.condition().requiredPhase())
                    .conditionJson(toJson(ruleYaml.condition()))
                    .sortOrder(ruleYaml.sortOrder())
                    .build());
        }

        for (Evidence evidence : evidencesByCode.values()) {
            if (!unlockRulesByEvidenceCode.containsKey(evidence.getCode())) {
                evidenceUnlockRuleRepository.save(EvidenceUnlockRule.builder()
                        .scenarioId(scenarioId)
                        .evidenceId(evidence.getId())
                        .evidenceCode(evidence.getCode())
                        .unlockType(evidence.getUnlockType().name())
                        .requiredPhase(evidence.getUnlockPhase())
                        .conditionJson(toJson(Map.of("requiredPhase", nullSafe(evidence.getUnlockPhase()))))
                        .sortOrder(evidence.getSortOrder())
                        .build());
            }
        }
    }

    private void saveNpcPolicies(ScenarioYaml yaml,
                                 Long scenarioId,
                                 Map<String, Suspect> suspectsByCode,
                                 Map<String, Evidence> evidencesByCode) {
        for (ScenarioYaml.NpcPolicyYaml policyYaml : listOf(yaml.npcPolicies())) {
            Suspect suspect = suspectsByCode.get(policyYaml.characterCode());
            if (suspect == null) {
                throw new ScenarioImportException("npcPolicy character not found: " + policyYaml.characterCode());
            }

            ScenarioYaml.PromptSafeKnowledgeYaml knowledge = policyYaml.promptSafeKnowledge();
            npcKnowledgeProfileRepository.save(NpcKnowledgeProfile.builder()
                    .scenarioId(scenarioId)
                    .suspectId(suspect.getId())
                    .characterCode(policyYaml.characterCode())
                    .publicAlibi(policyYaml.publicAlibi())
                    .selfRole(knowledge == null ? null : knowledge.selfRole())
                    .directKnowledgeJson(toJson(knowledge == null ? List.of() : knowledge.directKnowledge()))
                    .inferredKnowledgeJson(toJson(knowledge == null ? List.of() : knowledge.inferredKnowledge()))
                    .forbiddenKnowledgeJson(toJson(knowledge == null ? List.of() : knowledge.forbiddenKnowledge()))
                    .stagePoliciesJson(toJson(policyYaml.stagePolicies()))
                    .build());

            saveDefaultResponsePolicies(suspect.getId(), policyYaml);
            saveEvidenceReactionPolicies(suspect.getId(), policyYaml, evidencesByCode);
        }
    }

    private void saveDefaultResponsePolicies(Long suspectId, ScenarioYaml.NpcPolicyYaml policyYaml) {
        listOf(policyYaml.stagePolicies()).stream()
                .filter(stage -> "DEFAULT".equals(stage.stage()))
                .findFirst()
                .ifPresent(stage -> suspectResponsePolicyRepository.save(SuspectResponsePolicy.builder()
                        .suspectId(suspectId)
                        .conditionKey("DEFAULT")
                        .policyText(stage.policyText())
                        .allowedFacts(toJson(stage.allowedFacts()))
                        .forbiddenFacts(toJson(stage.forbiddenFacts()))
                        .tone(stage.tone())
                        .priority(0)
                        .build()));
    }

    private void saveEvidenceReactionPolicies(Long suspectId,
                                              ScenarioYaml.NpcPolicyYaml policyYaml,
                                              Map<String, Evidence> evidencesByCode) {
        for (ScenarioYaml.NpcEvidenceReactionPolicyYaml reaction : listOf(policyYaml.evidenceReactionPolicies())) {
            Evidence evidence = evidencesByCode.get(reaction.evidenceCode());
            if (evidence == null) {
                throw new ScenarioImportException("npcPolicy reaction evidence not found: " + reaction.evidenceCode());
            }
            suspectResponsePolicyRepository.save(SuspectResponsePolicy.builder()
                    .suspectId(suspectId)
                    .conditionKey("PRESENTED_" + reaction.evidenceCode())
                    .requiredEvidenceIds(toJson(List.of(evidence.getId())))
                    .presentedEvidenceId(evidence.getId())
                    .policyText(reaction.policyText())
                    .allowedFacts(toJson(reaction.allowedFacts()))
                    .forbiddenFacts(toJson(reaction.forbiddenFacts()))
                    .tone(reaction.tone())
                    .priority(reaction.priority() == null ? 100 : reaction.priority())
                    .build());
        }
    }

    private void saveAssets(ScenarioYaml yaml, Long scenarioId) {
        for (ScenarioYaml.AssetYaml assetYaml : listOf(yaml.assets())) {
            scenarioAssetRepository.save(ScenarioAsset.builder()
                    .scenarioId(scenarioId)
                    .assetKey(assetYaml.assetKey())
                    .type(assetYaml.type())
                    .targetKind(assetYaml.targetKind())
                    .targetCode(assetYaml.targetCode())
                    .s3ObjectKey(assetYaml.s3ObjectKey())
                    .contentType(assetYaml.contentType())
                    .sourceStatus(assetYaml.sourceStatus())
                    .altText(assetYaml.altText())
                    .build());
        }
    }

    private EvidenceImportance toImportance(String baseRole) {
        if (baseRole == null) {
            return EvidenceImportance.NORMAL;
        }
        if (baseRole.contains("DIRTY_FAKE")) {
            return EvidenceImportance.FAKE;
        }
        if (baseRole.contains("KEY") || baseRole.contains("FATAL")) {
            return EvidenceImportance.CORE;
        }
        if (baseRole.contains("MOTIVE") || baseRole.contains("UNLOCK")) {
            return EvidenceImportance.HIGH;
        }
        return EvidenceImportance.NORMAL;
    }

    private EvidenceUnlockType toUnlockType(ScenarioYaml.UnlockRuleYaml unlockRule) {
        if (unlockRule == null || !hasText(unlockRule.unlockType())) {
            return EvidenceUnlockType.PHASE;
        }
        return enumValue(EvidenceUnlockType.class, unlockRule.unlockType(), "unlockRules[].unlockType");
    }

    private String toConditionJson(ScenarioYaml.EvidenceYaml evidenceYaml, ScenarioYaml.UnlockRuleYaml unlockRule) {
        if (unlockRule != null && unlockRule.condition() != null) {
            return toJson(unlockRule.condition());
        }
        return toJson(Map.of("requiredPhase", nullSafe(evidenceYaml.unlockPhase())));
    }

    private VariantType toVariantType(String variantCode) {
        String name = variantCode == null ? "" : variantCode.replaceFirst("^VARIANT_", "");
        return enumValue(VariantType.class, name, "variants[].code");
    }

    private boolean isOpeningPhase(String unlockPhase) {
        return "PHASE_0_OPENING".equals(unlockPhase);
    }

    private String toEvidenceIdCsv(Set<String> evidenceCodes, Map<String, Evidence> evidencesByCode) {
        return evidenceCodes.stream()
                .map(evidencesByCode::get)
                .filter(Objects::nonNull)
                .map(Evidence::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private Set<String> collectEvidenceCodes(Object value) {
        Set<String> codes = new LinkedHashSet<>();
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

    private String toJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception e) {
            throw new ScenarioImportException("JSON 직렬화에 실패했습니다.", e);
        }
    }

    private String toNullableJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new ScenarioImportException("JSON 직렬화에 실패했습니다.", e);
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private <E extends Enum<E>> E enumValue(Class<E> enumType, String value, String field) {
        try {
            return Enum.valueOf(enumType, value);
        } catch (Exception e) {
            throw new ScenarioImportException(field + " 값이 enum에 없습니다: " + value);
        }
    }

    private <T> List<T> listOf(List<T> source) {
        return source == null ? List.of() : source;
    }

    private Map<String, Object> mapOf(Map<String, Object> source) {
        return source == null ? Map.of() : source;
    }

    private <T> Map<String, T> mapByCode(List<T> items, Function<T, String> codeExtractor) {
        return items.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        codeExtractor,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }
}
