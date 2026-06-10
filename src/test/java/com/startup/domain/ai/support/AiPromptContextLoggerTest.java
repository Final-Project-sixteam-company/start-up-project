package com.startup.domain.ai.support;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.startup.domain.ai.client.AiCallContext;
import com.startup.domain.ai.dto.ChatTurn;
import com.startup.domain.ai.dto.EvidenceInfo;
import com.startup.domain.ai.dto.ResponsePolicyResult;
import com.startup.domain.ai.dto.SuspectProfile;
import com.startup.domain.ai.enums.AiFeatureType;
import com.startup.domain.ai.enums.QuestionType;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiPromptContextLoggerTest {

    private final AiPromptContextLogger logger = new AiPromptContextLogger();

    @Test
    void recordInterrogation_doesNotLogRawPromptAnswerOrQuestion() {
        Logger testLogger = (Logger) LoggerFactory.getLogger(AiPromptContextLogger.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        testLogger.addAppender(appender);

        try {
            logger.recordInterrogation(
                    new AiCallContext(
                            AiFeatureType.INTERROGATION,
                            "npc_interrogation_v1",
                            10L,
                            20L,
                            30L,
                            "NPC_SENTINEL_CODE"
                    ),
                    "deepseek",
                    "deepseek-v4-flash",
                    "RAW_SYSTEM_PROMPT_SENTINEL",
                    "RAW_USER_PROMPT_SENTINEL",
                    new SuspectProfile(
                            30L,
                            "NPC_SENTINEL_CODE",
                            "NPC_NAME_SENTINEL",
                            "역할",
                            "관계",
                            "프로필",
                            "진술",
                            "알리바이"
                    ),
                    List.of(new EvidenceInfo(1L, "EVIDENCE_TITLE_SENTINEL", "EVIDENCE_DESCRIPTION_SENTINEL")),
                    null,
                    new ResponsePolicyResult(
                            "DEFAULT",
                            "POLICY_TEXT_SENTINEL",
                            List.of("ALLOWED_FACT_SENTINEL"),
                            List.of("FORBIDDEN_FACT_SENTINEL"),
                            "TONE_SENTINEL"
                    ),
                    List.of(new ChatTurn("HISTORY_QUESTION_SENTINEL", "HISTORY_ANSWER_SENTINEL")),
                    "USER_QUESTION_SENTINEL",
                    QuestionType.FREE,
                    "template123"
            );
        } finally {
            testLogger.detachAppender(appender);
        }

        assertThat(appender.list).hasSize(1);
        String message = appender.list.get(0).getFormattedMessage();
        assertThat(message).contains("AI_CALL_CONTEXT");
        assertThat(message).contains("featureType=INTERROGATION");
        assertThat(message).contains("promptVersion=npc_interrogation_v1");
        assertThat(message).contains("templateHash=template123");
        assertThat(message).doesNotContain(
                "RAW_SYSTEM_PROMPT_SENTINEL",
                "RAW_USER_PROMPT_SENTINEL",
                "USER_QUESTION_SENTINEL",
                "HISTORY_QUESTION_SENTINEL",
                "HISTORY_ANSWER_SENTINEL",
                "EVIDENCE_TITLE_SENTINEL",
                "EVIDENCE_DESCRIPTION_SENTINEL",
                "POLICY_TEXT_SENTINEL",
                "ALLOWED_FACT_SENTINEL",
                "FORBIDDEN_FACT_SENTINEL",
                "NPC_NAME_SENTINEL",
                "NPC_SENTINEL_CODE",
                "scenarioId=10",
                "sessionId=20",
                "suspectId=30"
        );
    }

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
        assertThat(metrics.policyContextTokens()).isPositive();
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
