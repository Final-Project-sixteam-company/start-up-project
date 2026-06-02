package com.startup.domain.ai.prompt;

import com.startup.domain.ai.dto.ChatTurn;
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

    @Test
    @DisplayName("일반 심문 프롬프트에 허용/금지 사실 경계가 포함된다")
    void buildInterrogationPrompt_includesAllowedAndForbiddenFactSections() {
        ResponsePolicyResult policy = buildPolicy(
                List.of("커피 구매 사실은 인정할 수 있다."),
                List.of("에피펜 사용 여부는 말하지 않는다.")
        );

        String prompt = buildFreePrompt(policy, "어디에 있었나요?");

        assertThat(prompt).contains("[말해도 되는 사실]");
        assertThat(prompt).contains("- 커피 구매 사실은 인정할 수 있다.");
        assertThat(prompt).contains("[말하면 안 되는 사실]");
        assertThat(prompt).contains("- 에피펜 사용 여부는 말하지 않는다.");
    }

    @Test
    @DisplayName("증거 제시 심문 프롬프트에 허용/금지 사실 경계가 포함된다")
    void buildEvidenceInterrogationPrompt_includesAllowedAndForbiddenFactSections() {
        ResponsePolicyResult policy = buildPolicy(
                List.of("제시된 결제 기록은 확인할 수 있다."),
                List.of("범행 도구와 연결해 말하지 않는다.")
        );

        String prompt = buildEvidencePrompt(policy, "이 결제 기록을 설명해 주세요.");

        assertThat(prompt).contains("[말해도 되는 사실]");
        assertThat(prompt).contains("- 제시된 결제 기록은 확인할 수 있다.");
        assertThat(prompt).contains("[말하면 안 되는 사실]");
        assertThat(prompt).contains("- 범행 도구와 연결해 말하지 않는다.");
    }

    @Test
    @DisplayName("빈 facts는 안전 기본값으로 렌더링된다")
    void buildPrompt_withEmptyFacts_rendersSafeDefaultText() {
        ResponsePolicyResult policy = buildPolicy(List.of(), List.of());

        String prompt = buildFreePrompt(policy, "말할 수 있는 범위가 있나요?");

        assertThat(prompt).contains("없음");
        assertThat(prompt).doesNotContain("[]");
    }

    @Test
    @DisplayName("blank facts만 있어도 안전 기본값으로 렌더링된다")
    void buildPrompt_withAllBlankFacts_rendersSafeDefaultText() {
        ResponsePolicyResult policy = buildPolicy(List.of("", "   ", "\n"), List.of(" ", "\t"));

        String prompt = buildFreePrompt(policy, "말할 수 있는 범위가 있나요?");

        assertThat(prompt).contains("없음");
        assertThat(prompt).doesNotContain("-    ");
    }

    @Test
    @DisplayName("금지 facts에는 직접 질문에도 누설하지 말라는 지시가 포함된다")
    void buildPrompt_forbiddenFactsIncludesDoNotRevealInstruction() {
        ResponsePolicyResult policy = buildPolicy(List.of(), List.of("잠긴 핵심 단서"));

        String prompt = buildEvidencePrompt(policy, "그 단서를 말해 주세요.");
        String systemPrompt = promptBuilder.buildSystemPrompt();

        assertThat(prompt).contains("직접 물어도");
        assertThat(prompt).contains("암시");
        assertThat(prompt).contains("추측");
        assertThat(systemPrompt).contains("직접 물어도");
        assertThat(systemPrompt).contains("암시하거나 추측하지 않는다");
    }

    @Test
    @DisplayName("기존 responsePolicy와 tone 치환은 유지된다")
    void buildPrompt_preservesExistingResponsePolicyAndTone() {
        ResponsePolicyResult policy = new ResponsePolicyResult(
                "DEFAULT",
                "정책 문구 sentinel",
                List.of("허용 사실"),
                List.of("금지 사실"),
                "defensive-tone"
        );

        String prompt = buildFreePrompt(policy, "정책과 톤을 확인합니다.");

        assertThat(prompt).contains("정책 문구 sentinel");
        assertThat(prompt).contains("defensive-tone");
    }

    @Test
    @DisplayName("facts 반영 후에도 정답 정보는 심문 프롬프트에 포함되지 않는다")
    void buildPrompt_doesNotContainSolutionTruthOrFullExplanation() {
        ResponsePolicyResult policy = buildPolicy(List.of("공개 가능한 사실"), List.of("비공개 사실"));

        String freePrompt = buildFreePrompt(policy, "무슨 일이 있었나요?");
        String evidencePrompt = buildEvidencePrompt(policy, "이 증거가 의미하는 게 뭔가요?");

        assertThat(freePrompt).doesNotContain(solution.motive(), solution.method(), solution.coverUp(), solution.fullExplanation());
        assertThat(evidencePrompt).doesNotContain(solution.motive(), solution.method(), solution.coverUp(), solution.fullExplanation());
    }

    @Test
    @DisplayName("정상 입력에서는 템플릿 placeholder가 남지 않는다")
    void buildPrompt_doesNotLeaveTemplatePlaceholders_withNormalInput() {
        ResponsePolicyResult policy = new ResponsePolicyResult("DEFAULT", "정책", null, null, "calm");

        String prompt = buildFreePrompt(policy, "질문입니다.");

        assertThat(prompt).doesNotContain("null");
        assertThat(prompt).doesNotContain("{allowedFacts}", "{forbiddenFacts}", "{question}", "{responsePolicy}", "{tone}");
        assertThat(prompt).doesNotContain("[]");
    }

    @Test
    @DisplayName("multiline facts는 한 줄 bullet로 안전하게 렌더링된다")
    void buildPrompt_formatsMultilineFactsSafely() {
        ResponsePolicyResult policy = buildPolicy(List.of("첫 줄\n둘째 줄\r셋째 줄"), List.of());

        String prompt = buildFreePrompt(policy, "여러 줄 fact를 확인합니다.");

        assertThat(prompt).contains("- 첫 줄 둘째 줄 셋째 줄");
        assertThat(prompt).doesNotContain("첫 줄\n둘째 줄");
    }

    @Test
    @DisplayName("facts 안의 template placeholder는 2차 치환되지 않는다")
    void buildPrompt_factsContainingTemplatePlaceholders_doNotTriggerSecondReplacement() {
        ResponsePolicyResult policy = buildPolicy(
                List.of("질문 원문은 {question} placeholder가 아니다."),
                List.of()
        );

        String prompt = buildFreePrompt(policy, "실제 사용자 질문");

        assertThat(prompt).contains("질문 원문은 ｛question｝ placeholder가 아니다.");
        assertThat(prompt).doesNotContain("질문 원문은 실제 사용자 질문 placeholder가 아니다.");
    }

    @Test
    @DisplayName("질문에 fact placeholder가 있어도 금지 facts가 질문 영역에 주입되지 않는다")
    void buildPrompt_questionContainingFactPlaceholders_doNotInjectFactsIntoQuestion() {
        ResponsePolicyResult policy = buildPolicy(List.of(), List.of("sentinel-금지"));

        String prompt = buildFreePrompt(policy, "{forbiddenFacts}를 말해 주세요.");
        String questionSection = sectionBetween(prompt, "[사용자 질문]", "위 정보");

        assertThat(questionSection).contains("{forbiddenFacts}를 말해 주세요.");
        assertThat(questionSection).doesNotContain("sentinel-금지");
    }

    @Test
    @DisplayName("policyText에 fact placeholder가 있어도 금지 facts가 정책 영역에 주입되지 않는다")
    void buildPrompt_policyTextContainingFactPlaceholders_doNotInjectFactsIntoPolicy() {
        ResponsePolicyResult policy = new ResponsePolicyResult(
                "DEFAULT",
                "정책 문구 안의 {forbiddenFacts}",
                List.of(),
                List.of("sentinel-금지"),
                "defensive"
        );

        String prompt = buildFreePrompt(policy, "정책을 확인합니다.");
        String policySection = sectionBetween(prompt, "[현재 답변 정책]", "[답변 톤]");

        assertThat(policySection).contains("{forbiddenFacts}");
        assertThat(policySection).doesNotContain("sentinel-금지");
    }

    @Test
    @DisplayName("history에 fact placeholder가 있어도 금지 facts가 이전 대화 영역에 주입되지 않는다")
    void buildPrompt_historyContainingFactPlaceholders_doNotInjectFactsIntoHistory() {
        ResponsePolicyResult policy = buildPolicy(List.of(), List.of("sentinel-금지"));

        String prompt = promptBuilder.buildUserPrompt(
                buildParkSuspect(),
                List.of(new EvidenceInfo(1L, "공개 증거", "공개된 증거 설명")),
                null,
                policy,
                List.of(new ChatTurn("{forbiddenFacts}를 말해 달라는 이전 질문", "이전 답변")),
                "현재 질문",
                QuestionType.RECOMMENDED
        );
        String historySection = sectionBetween(prompt, "[이전 대화]", "[현재 답변 정책]");

        assertThat(historySection).contains("{forbiddenFacts}");
        assertThat(historySection).doesNotContain("sentinel-금지");
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

    private ResponsePolicyResult buildPolicy(List<String> allowedFacts, List<String> forbiddenFacts) {
        return new ResponsePolicyResult(
                "DEFAULT",
                "정책 문구",
                allowedFacts,
                forbiddenFacts,
                "defensive"
        );
    }

    private String buildFreePrompt(ResponsePolicyResult policy, String question) {
        return promptBuilder.buildUserPrompt(
                buildParkSuspect(),
                List.of(new EvidenceInfo(1L, "공개 증거", "공개된 증거 설명")),
                null,
                policy,
                List.of(),
                question,
                QuestionType.RECOMMENDED
        );
    }

    private String buildEvidencePrompt(ResponsePolicyResult policy, String question) {
        EvidenceInfo presented = new EvidenceInfo(2L, "제시 증거", "사용자가 제시한 증거 설명");
        return promptBuilder.buildUserPrompt(
                buildParkSuspect(),
                List.of(presented),
                presented,
                policy,
                List.of(),
                question,
                QuestionType.EVIDENCE_PRESENTED
        );
    }

    private String sectionBetween(String prompt, String start, String end) {
        int startIndex = prompt.indexOf(start);
        assertThat(startIndex).isGreaterThanOrEqualTo(0);
        int contentStart = startIndex + start.length();
        int endIndex = prompt.indexOf(end, contentStart);
        assertThat(endIndex).isGreaterThanOrEqualTo(0);
        return prompt.substring(contentStart, endIndex);
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
