package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.ValidationCheckItem;
import com.startup.domain.ai.dto.ScenarioValidationData;
import com.startup.domain.ai.enums.ValidationSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedScenarioValidatorTest {

    private RuleBasedScenarioValidator validator;
    private MockScenarioDataReader dataReader;

    @BeforeEach
    void setUp() {
        validator = new RuleBasedScenarioValidator();
        dataReader = new MockScenarioDataReader();
    }

    @Test
    @DisplayName("공식 정본 데이터는 hard blocker가 없다")
    void officialScenario_hasNoHardBlocker() {
        RuleBasedScenarioValidator.RuleValidationResult result =
                validator.validate(dataReader.loadForValidation(1L));

        assertThat(result.hasHardBlocker()).isFalse();
    }

    @Test
    @DisplayName("공개 rule item은 4개다")
    void publicRuleItems_hasFourItems() {
        RuleBasedScenarioValidator.RuleValidationResult result =
                validator.validate(dataReader.loadForValidation(1L));

        assertThat(result.publicItems()).hasSize(4);
    }

    @Test
    @DisplayName("internal blocker는 maxScore 0이다")
    void internalBlockers_haveZeroMaxScore() {
        RuleBasedScenarioValidator.RuleValidationResult result =
                validator.validate(dataReader.loadForValidation(1L));

        assertThat(result.allItems())
                .filteredOn(item -> item.severity() == ValidationSeverity.HARD)
                .extracting(ValidationCheckItem::maxScore)
                .containsOnly(0);
    }

    @Test
    @DisplayName("all_alibi와 all_response_policy가 통과한다")
    void alibiAndResponsePolicy_pass() {
        RuleBasedScenarioValidator.RuleValidationResult result =
                validator.validate(dataReader.loadForValidation(1L));

        assertThat(result.publicItems())
                .filteredOn(item -> item.key().equals("all_alibi") || item.key().equals("all_response_policy"))
                .allMatch(ValidationCheckItem::passed);
    }

    @Test
    @DisplayName("solutionEvidences가 비어도 solution.keyEvidenceIds가 있으면 핵심 증거가 존재한다")
    void keyEvidenceIds_countAsKeyEvidence() {
        ScenarioValidationData data = dataReader.loadForValidation(1L);
        ScenarioValidationData withoutSolutionEvidences = new ScenarioValidationData(
                data.scenario(),
                data.victim(),
                data.locations(),
                data.suspects(),
                data.evidences(),
                data.hints(),
                data.timelineEvents(),
                data.solution(),
                List.of(),
                data.responsePolicies(),
                data.suspectSecrets()
        );

        RuleBasedScenarioValidator.RuleValidationResult result = validator.validate(withoutSolutionEvidences);

        assertThat(result.hasHardBlocker()).isFalse();
        assertThat(result.publicItems())
                .filteredOn(item -> item.key().equals("key_evidence_exist"))
                .singleElement()
                .matches(ValidationCheckItem::passed);
    }
}
