package com.startup.domain.scenario.importer;

import com.startup.domain.scenario.importer.yaml.ScenarioYaml;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioYamlLoaderValidatorTest {

    private final ScenarioYamlLoader loader = new ScenarioYamlLoader();
    private final ScenarioYamlValidator validator = new ScenarioYamlValidator();

    @Test
    void validYaml_loadsAndPassesValidation() throws IOException {
        ScenarioYaml yaml = load(SAMPLE_YAML);

        List<String> violations = validator.validate(yaml);

        assertThat(yaml.scenario().code()).isEqualTo("SCENARIO_TEST");
        assertThat(yaml.evidences()).hasSize(2);
        assertThat(yaml.timelineEvents()).hasSize(1);
        assertThat(yaml.timelineEvents().getFirst().relatedEvidenceCode()).isEqualTo("EVIDENCE_KEY");
        assertThat(yaml.evidences().getFirst().relatedCharacterCodes()).containsExactly("SUSPECT_TEST");
        assertThat(yaml.locations().getFirst().mapX()).isEqualTo(120);
        assertThat(yaml.locations().getFirst().mapY()).isEqualTo(80);
        assertThat(violations).isEmpty();
    }

    @Test
    void duplicateEvidenceCode_failsValidation() throws IOException {
        String invalidYaml = SAMPLE_YAML.replace(
                "  - code: EVIDENCE_SUPPORT",
                "  - code: EVIDENCE_KEY"
        );

        List<String> violations = validator.validate(load(invalidYaml));

        assertThat(violations).anyMatch(message -> message.contains("duplicate code in evidences: EVIDENCE_KEY"));
    }

    @Test
    void missingEvidenceLocation_failsValidation() throws IOException {
        String invalidYaml = SAMPLE_YAML.replace(
                "locationCode: LOC_TEST",
                "locationCode: LOC_MISSING"
        );

        List<String> violations = validator.validate(load(invalidYaml));

        assertThat(violations).anyMatch(message -> message.contains("references missing location: LOC_MISSING"));
    }

    @Test
    void missingRelatedCharacter_failsValidation() throws IOException {
        String invalidYaml = SAMPLE_YAML.replace(
                "relatedCharacterCodes:\n      - SUSPECT_TEST",
                "relatedCharacterCodes:\n      - SUSPECT_MISSING"
        );

        List<String> violations = validator.validate(load(invalidYaml));

        assertThat(violations).anyMatch(message -> message.contains(
                "relatedCharacterCodes references missing character: SUSPECT_MISSING"));
    }

    @Test
    void missingTimelineEvidence_failsValidation() throws IOException {
        String invalidYaml = SAMPLE_YAML.replace(
                "relatedEvidenceCode: EVIDENCE_KEY",
                "relatedEvidenceCode: EVIDENCE_MISSING"
        );

        List<String> violations = validator.validate(load(invalidYaml));

        assertThat(violations).anyMatch(message -> message.contains(
                "timelineEvent TIMELINE_TEST references missing related evidence: EVIDENCE_MISSING"));
    }

    @Test
    void invalidTimelineVisibility_failsValidation() throws IOException {
        String invalidYaml = SAMPLE_YAML.replace(
                "visibility: PUBLIC\n    isTrueEvent: true",
                "visibility: PULBIC\n    isTrueEvent: true"
        );

        List<String> violations = validator.validate(load(invalidYaml));

        assertThat(violations).anyMatch(message -> message.contains(
                "timelineEvents[TIMELINE_TEST].visibility must be one of"));
    }

    @Test
    void duplicateTimelineOrder_failsValidation() throws IOException {
        String invalidYaml = SAMPLE_YAML.replace(
                "timelineEvents:\n" +
                        "  - code: TIMELINE_TEST",
                "timelineEvents:\n" +
                        "  - code: TIMELINE_DUPLICATE\n" +
                        "    eventOrder: 10\n" +
                        "    eventTime: \"20:55\"\n" +
                        "    title: \"중복 순서\"\n" +
                        "    description: \"중복 순서\"\n" +
                        "    eventType: FACT\n" +
                        "    visibility: PUBLIC\n" +
                        "    isTrueEvent: true\n" +
                        "    locationCode: LOC_TEST\n" +
                        "    relatedEvidenceCode: null\n" +
                        "    relatedCharacterCode: null\n\n" +
                        "  - code: TIMELINE_TEST"
        );

        List<String> violations = validator.validate(load(invalidYaml));

        assertThat(violations).anyMatch(message -> message.contains("duplicate timelineEvent eventOrder: 10"));
    }

    @Test
    void missingAssetTargetKind_failsValidation() throws IOException {
        String invalidYaml = SAMPLE_YAML.replace("    targetKind: EVIDENCE\n", "");

        List<String> violations = validator.validate(load(invalidYaml));

        assertThat(violations).anyMatch(message -> message.contains("targetKind is required"));
    }

    @Test
    void publishedYamlRequiresNonEmptyRootSections() throws IOException {
        String invalidYaml = SAMPLE_YAML
                .replace("contentStatus: DRAFT", "contentStatus: PUBLISHED")
                .replace("status: DRAFT", "status: PUBLISHED");
        invalidYaml = invalidYaml.substring(0, invalidYaml.indexOf("assets:"))
                + "assets: []\n";

        List<String> violations = validator.validate(load(invalidYaml));

        assertThat(violations).anyMatch(message -> message.contains("PUBLISHED requires non-empty assets"));
    }

    @Test
    void publishedYamlRequiresEnabledVariant() throws IOException {
        String invalidYaml = SAMPLE_YAML
                .replace("contentStatus: DRAFT", "contentStatus: PUBLISHED")
                .replace("status: DRAFT", "status: PUBLISHED")
                .replace("enabled: true", "enabled: false");

        List<String> violations = validator.validate(load(invalidYaml));

        assertThat(violations).anyMatch(message -> message.contains("at least one enabled variant"));
    }

    @Test
    void publishedEnabledVariantRequiresSolutionMethodSummary() throws IOException {
        String invalidYaml = SAMPLE_YAML
                .replace("contentStatus: DRAFT", "contentStatus: PUBLISHED")
                .replace("status: DRAFT", "status: PUBLISHED")
                .replace("      methodSummary: \"방법\"\n", "");

        List<String> violations = validator.validate(load(invalidYaml));

        assertThat(violations).anyMatch(message -> message.contains("solution.methodSummary is required"));
    }

    @Test
    void publishedYamlRequiresLocationCoordinates() throws IOException {
        String invalidYaml = SAMPLE_YAML
                .replace("contentStatus: DRAFT", "contentStatus: PUBLISHED")
                .replace("status: DRAFT", "status: PUBLISHED")
                .replace("    mapX: 120\n    mapY: 80\n", "");

        List<String> violations = validator.validate(load(invalidYaml));

        assertThat(violations).anyMatch(message -> message.contains("locations[LOC_TEST].mapX is required"));
        assertThat(violations).anyMatch(message -> message.contains("locations[LOC_TEST].mapY is required"));
    }

    @Test
    void nonCulpritEligibleCharacterAsCulprit_failsValidation() throws IOException {
        String invalidYaml = SAMPLE_YAML.replace(
                "culpritCode: SUSPECT_TEST",
                "culpritCode: WITNESS_TEST"
        );

        List<String> violations = validator.validate(load(invalidYaml));

        assertThat(violations).anyMatch(message -> message.contains("uses non-culprit-eligible character: WITNESS_TEST"));
    }

    private ScenarioYaml load(String yaml) throws IOException {
        Path tempFile = Files.createTempFile("scenario-yaml-test", ".yaml");
        Files.writeString(tempFile, yaml, StandardCharsets.UTF_8);
        try {
            return loader.load(tempFile);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private static final String SAMPLE_YAML = """
            metadata:
              schemaVersion: 1
              contentStatus: DRAFT
              locale: ko-KR
              sourceCanonRef: SAMPLE.md
              canonicalCodeRef: CANONICAL_CODES.md
              assetKeyPattern: official/sample/v1/{category}/{canonicalCode}.png
              draftScope: TEST

            scenario:
              code: SCENARIO_TEST
              version: "1.0.0"
              title: "테스트 시나리오"
              description: "테스트 설명"
              synopsis: "테스트 개요"
              difficulty: NORMAL
              estimatedPlayTimeMinutes: 10
              scenarioType: OFFICIAL
              visibility: PUBLIC
              status: DRAFT
              culpritMode: RANDOM_REQUIRED

            victim:
              code: VICTIM_TEST
              name: "피해자"
              roleLabel: "테스트 피해자"
              deathLocationCode: LOC_TEST
              publicCauseOfDeathText: "원인 미상"

            locations:
              - code: LOC_TEST
                name: "테스트 장소"
                description: "테스트 장소 설명"
                mapX: 120
                mapY: 80
                sortOrder: 10

            characters:
              - code: SUSPECT_TEST
                name: "용의자"
                roleLabel: "테스트 용의자"
                characterType: SUSPECT
                culpritEligible: true
                publicProfile: "공개 프로필"
                publicAlibi: "공개 알리바이"
                sortOrder: 10
              - code: WITNESS_TEST
                name: "참고인"
                roleLabel: "테스트 참고인"
                characterType: WITNESS
                culpritEligible: false
                publicProfile: "공개 프로필"
                publicAlibi: "공개 알리바이"
                sortOrder: 20

            evidences:
              - code: EVIDENCE_KEY
                title: "핵심 증거"
                category: DOCUMENT
                unlockPhase: PHASE_0_OPENING
                locationCode: LOC_TEST
                oneLine: "핵심 증거"
                baseDetail: "핵심 증거 상세"
                imageAssetKey: official/sample/v1/evidence/EVIDENCE_KEY.png
                thumbnailAssetKey: official/sample/v1/evidence/EVIDENCE_KEY.thumb.png
                relatedCharacterCodes:
                  - SUSPECT_TEST
                tags:
                  - method
                sortOrder: 10
              - code: EVIDENCE_SUPPORT
                title: "보조 증거"
                category: DOCUMENT
                unlockPhase: PHASE_1_BASIC
                locationCode: LOC_TEST
                oneLine: "보조 증거"
                baseDetail: "보조 증거 상세"
                imageAssetKey: official/sample/v1/evidence/EVIDENCE_SUPPORT.png
                relatedCharacterCodes:
                  - WITNESS_TEST
                tags:
                  - support
                sortOrder: 20

            timelineEvents:
              - code: TIMELINE_TEST
                eventOrder: 10
                eventTime: "21:00"
                title: "테스트 타임라인"
                description: "테스트 타임라인 설명"
                eventType: FACT
                visibility: PUBLIC
                isTrueEvent: true
                locationCode: LOC_TEST
                relatedEvidenceCode: EVIDENCE_KEY
                relatedCharacterCode: SUSPECT_TEST

            evidenceVariantStates:
              - variantCode: VARIANT_TEST
                evidenceCode: EVIDENCE_KEY
                role: METHOD_KEY
                detailAppend: "테스트 modifier"
                proofDimensions:
                  - METHOD_PROOF

            variants:
              - code: VARIANT_TEST
                enabled: true
                weight: 1
                culpritCode: SUSPECT_TEST
                culpritName: "용의자"
                culpritRole: "테스트 용의자"
                solution:
                  motiveSummary: "동기"
                  methodSummary: "방법"
                  coverUpSummary: "은폐"
                  solutionText: "해설"
                  proofDimensions:
                    METHOD_PROOF:
                      primary:
                        - EVIDENCE_KEY
                misleadingEvidenceCodes:
                  - EVIDENCE_SUPPORT

            unlockRules:
              - evidenceCode: EVIDENCE_KEY
                unlockType: PHASE
                condition:
                  requiredPhase: PHASE_0_OPENING
                  requiredEvidenceCodes: []
                  requiredCharacterCode: null
                  requiredInterrogationTopic: null
                  hintFallbackAllowed: true
                sortOrder: 10
              - evidenceCode: EVIDENCE_SUPPORT
                unlockType: PHASE
                condition:
                  requiredPhase: PHASE_1_BASIC
                  requiredEvidenceCodes:
                    - EVIDENCE_KEY
                  requiredCharacterCode: null
                  requiredInterrogationTopic: null
                  hintFallbackAllowed: true
                sortOrder: 20

            npcPolicies:
              - characterCode: SUSPECT_TEST
                publicAlibi: "공개 알리바이"
                promptSafeKnowledge:
                  selfRole: "테스트 용의자"
                  directKnowledge: []
                  inferredKnowledge: []
                  forbiddenKnowledge:
                    - activeVariant
                    - culpritCode
                stagePolicies:
                  - stage: DEFAULT
                    policyText: "짧게 답한다."
                    allowedFacts:
                      - publicAlibi
                    forbiddenFacts:
                      - backend-only solution
                    tone: guarded
                evidenceReactionPolicies:
                  - evidenceCode: EVIDENCE_KEY
                    policyText: "증거 반응"
                    allowedFacts: []
                    forbiddenFacts:
                      - backend-only solution
                    tone: guarded
                    priority: 10

            scoring:
              totalScore: 100
              passScore: 70

            assets:
              - assetKey: official/sample/v1/evidence/EVIDENCE_KEY.png
                type: EVIDENCE_IMAGE
                targetKind: EVIDENCE
                targetCode: EVIDENCE_KEY
                s3ObjectKey: official/sample/v1/evidence/EVIDENCE_KEY.png
                contentType: image/png
                sourceLocalFile: images/EVIDENCE_KEY.png
                sourceStatus: READY
                altText: "핵심 증거"
            """;
}
