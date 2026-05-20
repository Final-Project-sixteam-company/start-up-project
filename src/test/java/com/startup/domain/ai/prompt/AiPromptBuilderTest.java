package com.startup.domain.ai.prompt;

import com.startup.domain.ai.dto.EvidenceInfo;
import com.startup.domain.ai.dto.ResponsePolicyResult;
import com.startup.domain.ai.dto.SolutionInfo;
import com.startup.domain.ai.dto.SuspectProfile;
import com.startup.domain.ai.enums.QuestionType;
import com.startup.domain.ai.support.MockSolutionReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiPromptBuilderTest {

    private AiPromptBuilder promptBuilder;
    private SolutionInfo solution;

    @BeforeEach
    void setUp() {
        promptBuilder = new AiPromptBuilder(
                new ClassPathResource("prompts/interrogation_system_prompt.txt"),
                new ClassPathResource("prompts/interrogation_user_prompt.txt"),
                new ClassPathResource("prompts/evidence_interrogation_user_prompt.txt"),
                new ClassPathResource("prompts/final_deduction_scoring_prompt.txt"),
                new ClassPathResource("prompts/scenario_validation_prompt.txt")
        );
        solution = new MockSolutionReader().findByScenarioId(1L);
    }

    @Test
    @DisplayName("일반 심문 프롬프트에 정답 정보가 포함되지 않는다")
    void freeInterrogationPrompt_doesNotContain_solutionData() {
        String prompt = buildSampleFreePrompt();

        assertThat(prompt).doesNotContain(solution.motive());
        assertThat(prompt).doesNotContain(solution.method());
        assertThat(prompt).doesNotContain(solution.coverUp());
        assertThat(prompt).doesNotContain(solution.fullExplanation());
    }

    @Test
    @DisplayName("증거 제시 프롬프트에 정답 정보가 포함되지 않는다")
    void evidencePrompt_doesNotContain_solutionData() {
        String prompt = buildSampleEvidencePrompt();

        assertThat(prompt).doesNotContain(solution.motive());
        assertThat(prompt).doesNotContain(solution.method());
        assertThat(prompt).doesNotContain(solution.coverUp());
        assertThat(prompt).doesNotContain(solution.fullExplanation());
    }

    @Test
    @DisplayName("심문 프롬프트에 용의자 이름은 정상 포함된다")
    void interrogationPrompt_contains_suspectName() {
        String prompt = buildSampleFreePrompt();

        assertThat(prompt).contains("박재민");
    }

    @Test
    @DisplayName("심문 프롬프트에 용의자 공개 진술은 정상 포함된다")
    void interrogationPrompt_contains_publicStatement() {
        String prompt = buildSampleFreePrompt();

        assertThat(prompt).contains("재무팀 자리에서 투자 자료를 검토");
    }

    @Test
    @DisplayName("시스템 프롬프트에 정답 정보가 포함되지 않는다")
    void systemPrompt_doesNotContain_solutionData() {
        String systemPrompt = promptBuilder.buildSystemPrompt();

        assertThat(systemPrompt).doesNotContain(solution.motive());
        assertThat(systemPrompt).doesNotContain(solution.method());
        assertThat(systemPrompt).doesNotContain(solution.coverUp());
        assertThat(systemPrompt).doesNotContain(solution.fullExplanation());
    }

    private String buildSampleFreePrompt() {
        SuspectProfile suspect = buildParkSuspect();
        ResponsePolicyResult policy = new ResponsePolicyResult(
                "DEFAULT",
                "커피 구매는 인정하지만 피해자에게 준 것은 아니라고 주장한다.",
                List.of(),
                List.of(),
                "defensive"
        );

        return promptBuilder.buildUserPrompt(
                suspect,
                List.of(new EvidenceInfo(1L, "사건 현장 사진", "데모룸에서 촬영된 현장 사진")),
                null,
                policy,
                List.of(),
                "어젯밤 어디 계셨나요?",
                QuestionType.RECOMMENDED
        );
    }

    private String buildSampleEvidencePrompt() {
        SuspectProfile suspect = buildParkSuspect();
        EvidenceInfo presented = new EvidenceInfo(
                1L,
                "사건 현장 사진",
                "데모룸에서 촬영된 현장 사진"
        );
        ResponsePolicyResult policy = new ResponsePolicyResult(
                "DEFAULT",
                "제시된 증거에 대해 방어적으로 답변한다.",
                List.of(),
                List.of(),
                "defensive"
        );

        return promptBuilder.buildUserPrompt(
                suspect,
                List.of(presented),
                presented,
                policy,
                List.of(),
                "이 증거에 대해 설명해 주세요.",
                QuestionType.EVIDENCE_PRESENTED
        );
    }

    private SuspectProfile buildParkSuspect() {
        return new SuspectProfile(
                1L,
                "박재민",
                "CFO",
                "피해자와 공동 창업",
                "데모데이 전날 투자 자료를 정리하고 있었습니다.",
                "당시 재무팀 자리에서 투자 자료를 검토하고 있었습니다.",
                "재무/투자 총괄"
        );
    }
}
