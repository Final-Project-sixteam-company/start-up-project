package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.AiFeedbackResult;
import com.startup.domain.ai.dto.ScoringCriteria;
import com.startup.domain.ai.dto.ScoringResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FallbackFeedbackGenerator {

    public AiFeedbackResult generate(ScoringResult result, ScoringCriteria criteria) {
        List<String> matched = new ArrayList<>();
        List<String> missed = new ArrayList<>();

        evaluateItem(result.culpritCorrect(), result.culpritScore(), criteria.culpritMaxScore(),
                "범인을 정확히 지목했습니다.",
                null,
                "범인을 찾지 못했습니다. 진범은 다른 인물입니다.",
                matched, missed);

        evaluateItem(result.methodScore() == criteria.method().maxScore(),
                result.methodScore(), criteria.method().maxScore(),
                "범행 방법을 정확히 파악했습니다.",
                "범행 방법의 일부를 파악했지만, 세부 사항이 부족합니다.",
                "범행 방법을 파악하지 못했습니다.",
                matched, missed);

        evaluateItem(result.motiveScore() == criteria.motive().maxScore(),
                result.motiveScore(), criteria.motive().maxScore(),
                "범행 동기를 정확히 설명했습니다.",
                "범행 동기의 방향은 맞지만, 핵심 근거가 부족합니다.",
                "범행 동기를 파악하지 못했습니다.",
                matched, missed);

        evaluateItem(result.coverUpScore() == criteria.coverUp().maxScore(),
                result.coverUpScore(), criteria.coverUp().maxScore(),
                "은폐 방법을 정확히 설명했습니다.",
                "은폐 방법의 일부를 파악했습니다.",
                "은폐 방법을 파악하지 못했습니다.",
                matched, missed);

        evaluateItem(result.evidenceScore() == criteria.evidenceMaxScore(),
                result.evidenceScore(), criteria.evidenceMaxScore(),
                "결정적 증거를 정확히 선택했습니다.",
                "일부 결정적 증거를 선택했지만, 빠진 증거가 있습니다.",
                "결정적 증거를 선택하지 못했습니다.",
                matched, missed);

        String feedback = buildSummary(matched, missed);
        return new AiFeedbackResult(matched, missed, feedback);
    }

    private void evaluateItem(boolean isMax, int score, int maxScore,
                              String fullText, String partialText, String zeroText,
                              List<String> matched, List<String> missed) {
        if (isMax) {
            matched.add(fullText);
        } else if (score > 0 && partialText != null) {
            matched.add(partialText);
            missed.add(partialText);
        } else if (score > 0) {
            matched.add(fullText);
        } else {
            missed.add(zeroText);
        }
    }

    private String buildSummary(List<String> matched, List<String> missed) {
        if (missed.isEmpty()) {
            return "모든 추리를 정확하게 완료했습니다.";
        }
        if (matched.isEmpty()) {
            return "추리의 핵심 요소를 대부분 놓쳤습니다. 증거를 다시 검토해 보세요.";
        }
        return "전반적으로 핵심 추리는 정확하지만, 일부 세부 사항이 부족했습니다.";
    }
}
