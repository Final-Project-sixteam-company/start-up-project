package com.startup.domain.scenario.importer;

import com.startup.domain.ai.repository.SuspectResponsePolicyRepository;
import com.startup.domain.scenario.importer.yaml.ScenarioYaml;
import com.startup.domain.scenario.repository.EvidenceRepository;
import com.startup.domain.scenario.repository.EvidenceSuspectRepository;
import com.startup.domain.scenario.repository.EvidenceUnlockRuleRepository;
import com.startup.domain.scenario.repository.EvidenceVariantStateRepository;
import com.startup.domain.scenario.repository.NpcKnowledgeProfileRepository;
import com.startup.domain.scenario.repository.ScenarioAssetRepository;
import com.startup.domain.scenario.repository.ScenarioLocationRepository;
import com.startup.domain.scenario.repository.ScenarioRepository;
import com.startup.domain.scenario.repository.ScenarioVariantRepository;
import com.startup.domain.scenario.repository.SuspectRepository;
import com.startup.domain.scenario.repository.VariantSolutionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ScenarioYamlImportServiceTest {

    @Autowired
    private ScenarioYamlImportService importService;

    @Autowired
    private ScenarioRepository scenarioRepository;

    @Autowired
    private SuspectRepository suspectRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private EvidenceSuspectRepository evidenceSuspectRepository;

    @Autowired
    private ScenarioVariantRepository variantRepository;

    @Autowired
    private VariantSolutionRepository solutionRepository;

    @Autowired
    private EvidenceVariantStateRepository stateRepository;

    @Autowired
    private EvidenceUnlockRuleRepository unlockRuleRepository;

    @Autowired
    private NpcKnowledgeProfileRepository npcKnowledgeProfileRepository;

    @Autowired
    private SuspectResponsePolicyRepository policyRepository;

    @Autowired
    private ScenarioAssetRepository assetRepository;

    @Autowired
    private ScenarioLocationRepository locationRepository;

    @Test
    void importYaml_savesScenarioGraphAndSkipsSameHash() {
        ScenarioYaml yaml = sampleYaml();

        ScenarioImportResult imported = importService.importYaml(yaml, "hash-a", "test");
        ScenarioImportResult skipped = importService.importYaml(yaml, "hash-a", "test");

        assertThat(imported.status()).isEqualTo(ScenarioImportResult.Status.IMPORTED);
        assertThat(skipped.status()).isEqualTo(ScenarioImportResult.Status.SKIPPED);
        assertThat(scenarioRepository.count()).isEqualTo(1);
        assertThat(suspectRepository.count()).isEqualTo(2);
        assertThat(evidenceRepository.count()).isEqualTo(2);
        assertThat(evidenceSuspectRepository.count()).isEqualTo(2);
        assertThat(variantRepository.count()).isEqualTo(1);
        assertThat(solutionRepository.count()).isEqualTo(1);
        assertThat(stateRepository.count()).isEqualTo(1);
        assertThat(unlockRuleRepository.count()).isEqualTo(2);
        assertThat(npcKnowledgeProfileRepository.count()).isEqualTo(1);
        assertThat(policyRepository.count()).isEqualTo(1);
        assertThat(assetRepository.count()).isEqualTo(1);

        var variant = variantRepository
                .findFirstByScenarioIdAndIsActiveTrueOrderBySortOrderAsc(imported.scenarioId())
                .orElseThrow();
        var solution = solutionRepository.findByVariantId(variant.getId()).orElseThrow();
        assertThat(variant.getVariantName()).isEqualTo("용의자 Variant");
        assertThat(solution.getCulpritName()).isEqualTo("용의자");
        assertThat(solution.getCulpritRole()).isEqualTo("비서");
        assertThat(solution.getMethod()).isEqualTo("방법");

        var location = locationRepository.findByScenarioIdAndCode(imported.scenarioId(), "LOC_ROOM").orElseThrow();
        assertThat(location.getMapX()).isEqualTo(120);
        assertThat(location.getMapY()).isEqualTo(80);
    }

    @Test
    void importYaml_rejectsSameVersionDifferentHash() {
        ScenarioYaml yaml = sampleYaml();

        importService.importYaml(yaml, "hash-a", "test");

        assertThatThrownBy(() -> importService.importYaml(yaml, "hash-b", "test"))
                .isInstanceOf(ScenarioImportException.class)
                .hasMessageContaining("version");
    }

    private ScenarioYaml sampleYaml() {
        return new ScenarioYaml(
                new ScenarioYaml.MetadataYaml(
                        1,
                        "DRAFT",
                        "ko-KR",
                        "canon.md",
                        "brief.md",
                        "codes.md",
                        "asset-map.csv",
                        "official/test/v1/{category}/{canonicalCode}.png",
                        "TEST"
                ),
                new ScenarioYaml.ScenarioSectionYaml(
                        "SCENARIO_IMPORT_TEST",
                        "1.0.0",
                        "테스트 시나리오",
                        "YAML import 테스트",
                        "테스트용 사건",
                        "test",
                        "NORMAL",
                        30,
                        "OFFICIAL",
                        "PUBLIC",
                        "DRAFT",
                        "RANDOM_REQUIRED",
                        "VARIANT_PROOF",
                        "REFERENCE_ONLY",
                        "PHASE_BASED_EVIDENCE_TAB",
                        "official/test/v1/scenario/SCENARIO_IMPORT_TEST.cover.png",
                        "official/test/v1/scenario/SCENARIO_IMPORT_TEST.map.png"
                ),
                new ScenarioYaml.VictimYaml(
                        "VICTIM_TEST",
                        "피해자",
                        "피해자 역할",
                        "공개 프로필",
                        "LOC_ROOM",
                        "방에서 발견",
                        "원인 미상",
                        "official/test/v1/victims/VICTIM_TEST.png"
                ),
                List.of(new ScenarioYaml.LocationYaml(
                        "LOC_ROOM",
                        "방",
                        "1F",
                        "테스트 방",
                        null,
                        120,
                        80,
                        10
                )),
                List.of(
                        new ScenarioYaml.CharacterYaml(
                                "SUSPECT_SECRETARY",
                                "용의자",
                                "비서",
                                "CULPRIT_ELIGIBLE",
                                true,
                                "공개 프로필",
                                "공개 알리바이",
                                "정중함",
                                "official/test/v1/characters/SUSPECT_SECRETARY.png",
                                10
                        ),
                        new ScenarioYaml.CharacterYaml(
                                "WITNESS_TEST",
                                "목격자",
                                "목격자",
                                "NEUTRAL_WITNESS",
                                false,
                                "공개 프로필",
                                "공개 알리바이",
                                "조심스러움",
                                null,
                                20
                        )
                ),
                List.of(
                        new ScenarioYaml.EvidenceYaml(
                                "EVIDENCE_OPENING",
                                "초기 증거",
                                "SCENE",
                                "PHASE_0_OPENING",
                                "LOC_ROOM",
                                "COMMON",
                                "초기 공개",
                                "초기 공개 상세",
                                "official/test/v1/evidence/EVIDENCE_OPENING.png",
                                null,
                                List.of("SUSPECT_SECRETARY"),
                                List.of("opening"),
                                10
                        ),
                        new ScenarioYaml.EvidenceYaml(
                                "EVIDENCE_KEY",
                                "핵심 증거",
                                "DOCUMENT",
                                "PHASE_2_SYSTEM_LOGS",
                                "LOC_ROOM",
                                "METHOD_KEY",
                                "핵심 단서",
                                "핵심 단서 상세",
                                "official/test/v1/evidence/EVIDENCE_KEY.png",
                                "official/test/v1/evidence/EVIDENCE_KEY.thumb.png",
                                List.of("SUSPECT_SECRETARY"),
                                List.of("method"),
                                20
                        )
                ),
                List.of(new ScenarioYaml.EvidenceVariantStateYaml(
                        "VARIANT_SECRETARY",
                        "EVIDENCE_KEY",
                        "METHOD_KEY",
                        null,
                        "Variant별 추가 설명",
                        List.of(),
                        List.of("METHOD_PROOF")
                )),
                List.of(new ScenarioYaml.VariantYaml(
                        "VARIANT_SECRETARY",
                        true,
                        true,
                        1,
                        "SUSPECT_SECRETARY",
                        null,
                        null,
                        null,
                        null,
                        null,
                        Map.of("time", "21:00"),
                        Map.of(
                                "motiveSummary", "동기",
                                "methodSummary", "방법",
                                "coverUpSummary", "은폐",
                                "solutionText", "정답 설명",
                                "proofDimensions", Map.of(
                                        "METHOD_PROOF", Map.of("primary", List.of("EVIDENCE_KEY"))
                                )
                        ),
                        List.of("EVIDENCE_OPENING")
                )),
                List.of(
                        new ScenarioYaml.UnlockRuleYaml(
                                "EVIDENCE_OPENING",
                                "PHASE",
                                new ScenarioYaml.UnlockConditionYaml("PHASE_0_OPENING", List.of(), null, null, false),
                                10
                        ),
                        new ScenarioYaml.UnlockRuleYaml(
                                "EVIDENCE_KEY",
                                "PHASE",
                                new ScenarioYaml.UnlockConditionYaml("PHASE_2_SYSTEM_LOGS", List.of("EVIDENCE_OPENING"), null, null, false),
                                20
                        )
                ),
                List.of(new ScenarioYaml.NpcPolicyYaml(
                        "SUSPECT_SECRETARY",
                        "공개 알리바이",
                        new ScenarioYaml.PromptSafeKnowledgeYaml(
                                "비서",
                                List.of("공개 지식"),
                                List.of(),
                                List.of("culpritCode", "solutionText")
                        ),
                        List.of(new ScenarioYaml.NpcStagePolicyYaml(
                                "DEFAULT",
                                "공개 정보만 답한다.",
                                List.of("publicProfile", "publicAlibi", "unlockedEvidenceOnly"),
                                List.of("backend-only solution"),
                                "정중함"
                        )),
                        List.of()
                )),
                Map.of("maxScore", 100),
                List.of(new ScenarioYaml.AssetYaml(
                        "official/test/v1/evidence/EVIDENCE_KEY.png",
                        "IMAGE",
                        "EVIDENCE",
                        "EVIDENCE_KEY",
                        "official/test/v1/evidence/EVIDENCE_KEY.png",
                        "image/png",
                        "evidence-key.png",
                        "READY",
                        "핵심 증거 이미지"
                ))
        );
    }
}
