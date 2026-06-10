package com.startup.domain.ai.support;

import com.startup.domain.ai.client.AiCallContext;
import com.startup.domain.ai.dto.ChatTurn;
import com.startup.domain.ai.dto.EvidenceInfo;
import com.startup.domain.ai.dto.ResponsePolicyResult;
import com.startup.domain.ai.dto.SuspectProfile;
import com.startup.domain.ai.enums.QuestionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

@Slf4j
@Component
public class AiPromptContextLogger {

    private static final String UNKNOWN = "unknown";
    private static final int LOW_CARDINALITY_MAX_LENGTH = 80;

    public void recordInterrogation(AiCallContext context,
                                    String provider,
                                    String model,
                                    String systemPrompt,
                                    String userPrompt,
                                    SuspectProfile suspect,
                                    List<EvidenceInfo> revealedEvidences,
                                    EvidenceInfo presentedEvidence,
                                    ResponsePolicyResult policy,
                                    List<ChatTurn> history,
                                    String question,
                                    QuestionType questionType,
                                    String templateHash) {
        AiPromptContextMetrics metrics = summarizeInterrogation(
                systemPrompt,
                userPrompt,
                suspect,
                revealedEvidences,
                presentedEvidence,
                policy,
                history,
                question,
                questionType
        );

        log.info("AI_CALL_CONTEXT featureType={} provider={} model={} promptVersion={} "
                        + "systemRuleTokens={} policyContextTokens={} npcProfileTokens={} "
                        + "evidenceContextTokens={} historyTokens={} questionTokens={} "
                        + "promptCharLength={} historyTurns={} includedEvidenceCount={} templateHash={}",
                context.featureType(),
                safeLowCardinality(provider),
                safeLowCardinality(model),
                safeLowCardinality(context.promptVersion()),
                metrics.systemRuleTokens(),
                metrics.policyContextTokens(),
                metrics.npcProfileTokens(),
                metrics.evidenceContextTokens(),
                metrics.historyTokens(),
                metrics.questionTokens(),
                metrics.promptCharLength(),
                metrics.historyTurns(),
                metrics.includedEvidenceCount(),
                safeLowCardinality(templateHash));
    }

    AiPromptContextMetrics summarizeInterrogation(String systemPrompt,
                                                  String userPrompt,
                                                  SuspectProfile suspect,
                                                  List<EvidenceInfo> revealedEvidences,
                                                  EvidenceInfo presentedEvidence,
                                                  ResponsePolicyResult policy,
                                                  List<ChatTurn> history,
                                                  String question,
                                                  QuestionType questionType) {
        List<EvidenceInfo> includedEvidences = uniqueEvidences(revealedEvidences, presentedEvidence);
        return new AiPromptContextMetrics(
                estimateTokens(systemPrompt),
                estimateTokens(policyContext(policy, questionType)),
                estimateTokens(npcProfileContext(suspect, questionType)),
                estimateTokens(evidenceContext(includedEvidences)),
                estimateTokens(historyContext(history)),
                estimateTokens(question),
                length(systemPrompt) + length(userPrompt),
                history == null ? 0 : history.size(),
                includedEvidences.size()
        );
    }

    private String policyContext(ResponsePolicyResult policy, QuestionType questionType) {
        if (policy == null) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("\n");
        add(joiner, policy.policyText());
        addAll(joiner, policy.allowedFacts());
        if (questionType != QuestionType.EVIDENCE_PRESENTED) {
            add(joiner, policy.tone());
        }
        return joiner.toString();
    }

    private String npcProfileContext(SuspectProfile suspect, QuestionType questionType) {
        if (suspect == null) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("\n");
        add(joiner, suspect.name());
        add(joiner, suspect.role());
        add(joiner, suspect.alibi());
        if (questionType != QuestionType.EVIDENCE_PRESENTED) {
            add(joiner, suspect.relationToVictim());
            add(joiner, suspect.publicProfile());
            add(joiner, suspect.publicStatement());
        }
        return joiner.toString();
    }

    private String evidenceContext(List<EvidenceInfo> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("\n");
        for (EvidenceInfo evidence : evidences) {
            if (evidence == null) {
                continue;
            }
            add(joiner, evidence.title());
            add(joiner, evidence.description());
        }
        return joiner.toString();
    }

    private String historyContext(List<ChatTurn> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("\n");
        for (ChatTurn turn : history) {
            if (turn == null) {
                continue;
            }
            add(joiner, turn.question());
            add(joiner, turn.answer());
        }
        return joiner.toString();
    }

    private List<EvidenceInfo> uniqueEvidences(List<EvidenceInfo> revealedEvidences, EvidenceInfo presentedEvidence) {
        List<EvidenceInfo> result = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();
        if (revealedEvidences != null) {
            for (EvidenceInfo evidence : revealedEvidences) {
                addEvidence(result, seenIds, evidence);
            }
        }
        addEvidence(result, seenIds, presentedEvidence);
        return result;
    }

    private void addEvidence(List<EvidenceInfo> result, Set<Long> seenIds, EvidenceInfo evidence) {
        if (evidence == null) {
            return;
        }
        Long id = evidence.id();
        if (id != null && !seenIds.add(id)) {
            return;
        }
        result.add(evidence);
    }

    private void addAll(StringJoiner joiner, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            add(joiner, value);
        }
    }

    private void add(StringJoiner joiner, String value) {
        if (value != null && !value.isBlank()) {
            joiner.add(value.strip());
        }
    }

    private int estimateTokens(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        int tokens = 0;
        int asciiRunLength = 0;
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            if (Character.isWhitespace(codePoint)) {
                tokens += estimateAsciiRun(asciiRunLength);
                asciiRunLength = 0;
            } else if (isCjkOrHangul(codePoint)) {
                tokens += estimateAsciiRun(asciiRunLength);
                asciiRunLength = 0;
                tokens += 1;
            } else {
                asciiRunLength += Character.charCount(codePoint);
            }
            i += Character.charCount(codePoint);
        }
        tokens += estimateAsciiRun(asciiRunLength);
        return tokens;
    }

    private int estimateAsciiRun(int length) {
        if (length <= 0) {
            return 0;
        }
        return Math.max(1, (length + 3) / 4);
    }

    private boolean isCjkOrHangul(int codePoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        return block == Character.UnicodeBlock.HANGUL_SYLLABLES
                || block == Character.UnicodeBlock.HANGUL_JAMO
                || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA;
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }

    private String safeLowCardinality(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        String normalized = value.strip().replaceAll("[^A-Za-z0-9._:-]", "_");
        if (normalized.isBlank()) {
            return UNKNOWN;
        }
        return normalized.length() <= LOW_CARDINALITY_MAX_LENGTH
                ? normalized
                : normalized.substring(0, LOW_CARDINALITY_MAX_LENGTH);
    }
}
