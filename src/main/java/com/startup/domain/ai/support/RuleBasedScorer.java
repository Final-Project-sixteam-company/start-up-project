package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.FinalDeductionRequest;
import com.startup.domain.ai.dto.ScoringCriteria;
import com.startup.domain.ai.dto.ScoringCriteria.KeywordCriteria;
import com.startup.domain.ai.dto.ScoringResult;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class RuleBasedScorer {

    public ScoringResult score(FinalDeductionRequest request, ScoringCriteria criteria) {
        int culpritScore = scoreCulprit(request.selectedCulpritId(), criteria);
        int methodScore = scoreKeywords(request.methodText(), criteria.method());
        int motiveScore = scoreKeywords(request.motiveText(), criteria.motive());
        int coverUpScore = scoreKeywords(request.coverUpText(), criteria.coverUp());
        int evidenceMatchCount = countEvidenceMatch(request.selectedEvidenceIds(), criteria.keyEvidenceIds());
        int evidenceScore = scoreEvidence(evidenceMatchCount, criteria);

        int totalScore = culpritScore + methodScore + motiveScore + coverUpScore + evidenceScore;

        return new ScoringResult(
                totalScore,
                culpritScore,
                request.selectedCulpritId().equals(criteria.culpritSuspectId()),
                methodScore,
                motiveScore,
                coverUpScore,
                evidenceScore,
                evidenceMatchCount
        );
    }

    private int scoreCulprit(Long selectedId, ScoringCriteria criteria) {
        return selectedId.equals(criteria.culpritSuspectId()) ? criteria.culpritMaxScore() : 0;
    }

    private int scoreKeywords(String text, KeywordCriteria criteria) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        String lowerText = text.toLowerCase();
        long matchCount = criteria.keywords().stream()
                .filter(kw -> lowerText.contains(kw.toLowerCase()))
                .count();

        if (matchCount >= criteria.minMatch()) {
            return criteria.maxScore();
        }
        return (int) (criteria.maxScore() * matchCount / criteria.minMatch());
    }

    private int countEvidenceMatch(List<Long> selected, List<Long> keyIds) {
        if (selected == null || selected.isEmpty()) {
            return 0;
        }
        Set<Long> keySet = new HashSet<>(keyIds);
        return (int) selected.stream().filter(keySet::contains).count();
    }

    private int scoreEvidence(int matchCount, ScoringCriteria criteria) {
        int denominator = Math.min(3, criteria.keyEvidenceIds().size());
        if (denominator == 0) {
            return 0;
        }
        return Math.min(criteria.evidenceMaxScore(),
                criteria.evidenceMaxScore() * matchCount / denominator);
    }
}
