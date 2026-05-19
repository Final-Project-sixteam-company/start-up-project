package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.SolutionInfo;
import com.startup.domain.ai.error.AiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockSolutionReaderTest {

    private MockSolutionReader reader;

    @BeforeEach
    void setUp() {
        reader = new MockSolutionReader();
    }

    @Test
    @DisplayName("시나리오 1번 조회 시 keyEvidenceIds 8개 반환")
    void scenario1_returnsCorrectKeyEvidenceIds() {
        SolutionInfo solution = reader.findByScenarioId(1L);

        assertThat(solution.keyEvidenceIds())
                .containsExactly(2L, 6L, 7L, 8L, 11L, 12L, 13L, 14L);
    }

    @Test
    @DisplayName("시나리오 1번 조회 시 모든 evidenceTitle이 null이 아님")
    void scenario1_allEvidenceTitlesNotNull() {
        SolutionInfo solution = reader.findByScenarioId(1L);

        assertThat(solution.evidenceTitles()).hasSize(8);
        solution.keyEvidenceIds().forEach(id ->
                assertThat(solution.evidenceTitles().get(id))
                        .as("evidenceTitle for id=%d", id)
                        .isNotNull()
                        .isNotBlank()
        );
    }

    @Test
    @DisplayName("범인 정보가 올바르게 설정됨")
    void scenario1_culpritInfoCorrect() {
        SolutionInfo solution = reader.findByScenarioId(1L);

        assertThat(solution.culpritSuspectId()).isEqualTo(1L);
        assertThat(solution.culpritName()).isEqualTo("박재민");
        assertThat(solution.culpritRole()).isEqualTo("CFO");
    }

    @Test
    @DisplayName("정답 텍스트 필드가 모두 존재")
    void scenario1_allTextFieldsPresent() {
        SolutionInfo solution = reader.findByScenarioId(1L);

        assertThat(solution.motive()).isNotBlank();
        assertThat(solution.method()).isNotBlank();
        assertThat(solution.coverUp()).isNotBlank();
        assertThat(solution.fullExplanation()).isNotBlank();
    }

    @Test
    @DisplayName("존재하지 않는 시나리오 ID 조회 시 AiException")
    void unknownScenario_throwsAiException() {
        assertThatThrownBy(() -> reader.findByScenarioId(999L))
                .isInstanceOf(AiException.class);
    }

    @Test
    @DisplayName("keyEvidenceIds와 evidenceTitles 키가 일치")
    void keyEvidenceIds_matchesTitleKeys() {
        SolutionInfo solution = reader.findByScenarioId(1L);

        List<Long> keyIds = solution.keyEvidenceIds();
        assertThat(solution.evidenceTitles().keySet())
                .containsExactlyInAnyOrderElementsOf(keyIds);
    }
}
