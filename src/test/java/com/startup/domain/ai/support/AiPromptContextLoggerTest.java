package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.ChatTurn;
import com.startup.domain.ai.dto.EvidenceInfo;
import com.startup.domain.ai.dto.ResponsePolicyResult;
import com.startup.domain.ai.dto.SuspectProfile;
import com.startup.domain.ai.enums.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiPromptContextLoggerTest {

    private final AiPromptContextLogger logger = new AiPromptContextLogger();

    @Test
    void summarizeInterrogation_countsPromptBlocksWithoutRawText() {
        EvidenceInfo evidence = new EvidenceInfo(1L, "영수증", "피해자 방 앞 카페 영수증");
        AiPromptContextMetrics metrics = logger.summarizeInterrogation(
                "시스템 규칙: 2문장 이내",
                "전체 사용자 프롬프트",
                new SuspectProfile(
                        10L,
                        "NPC_SECRETARY",
                        "문하연",
                        "비서실장",
                        "피해자의 측근",
                        "일정을 관리했다.",
                        "저녁 이후 집무실에 있었다.",
                        "21시 이후 사무실"
                ),
                List.of(evidence),
                evidence,
                new ResponsePolicyResult(
                        "DEFAULT",
                        "허용 범위 안에서만 답한다.",
                        List.of("영수증은 본인 것이 아니라고 주장할 수 있다."),
                        List.of("비공개 사실"),
                        "방어적"
                ),
                List.of(new ChatTurn("마지막으로 본 시간은?", "저녁 식사 때입니다.")),
                "영수증을 설명해 주세요.",
                QuestionType.EVIDENCE_PRESENTED
        );

        assertThat(metrics.systemRuleTokens()).isPositive();
        assertThat(metrics.scenarioContextTokens()).isPositive();
        assertThat(metrics.npcProfileTokens()).isPositive();
        assertThat(metrics.evidenceContextTokens()).isPositive();
        assertThat(metrics.historyTokens()).isPositive();
        assertThat(metrics.questionTokens()).isPositive();
        assertThat(metrics.promptCharLength()).isEqualTo("시스템 규칙: 2문장 이내".length() + "전체 사용자 프롬프트".length());
        assertThat(metrics.historyTurns()).isEqualTo(1);
        assertThat(metrics.includedEvidenceCount()).isEqualTo(1);
    }

    @Test
    void summarizeInterrogation_countsPresentedEvidenceWhenNotInRevealedList() {
        AiPromptContextMetrics metrics = logger.summarizeInterrogation(
                "",
                "",
                null,
                List.of(new EvidenceInfo(1L, "공개 증거", "설명")),
                new EvidenceInfo(2L, "제시 증거", "추가 설명"),
                null,
                List.of(),
                "",
                QuestionType.EVIDENCE_PRESENTED
        );

        assertThat(metrics.includedEvidenceCount()).isEqualTo(2);
        assertThat(metrics.evidenceContextTokens()).isPositive();
    }
}
