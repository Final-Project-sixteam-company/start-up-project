package com.startup.domain.ai.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeductionScoringDefaults {

    private final int culpritMaxScore;
    private final int evidenceMaxScore;
    private final int methodMaxScore;
    private final int motiveMaxScore;
    private final int coverUpMaxScore;

    public DeductionScoringDefaults(
            @Value("${caselab.scoring.defaults.culprit-max-score:30}") int culpritMaxScore,
            @Value("${caselab.scoring.defaults.evidence-max-score:15}") int evidenceMaxScore,
            @Value("${caselab.scoring.defaults.method-max-score:25}") int methodMaxScore,
            @Value("${caselab.scoring.defaults.motive-max-score:20}") int motiveMaxScore,
            @Value("${caselab.scoring.defaults.cover-up-max-score:10}") int coverUpMaxScore) {
        this.culpritMaxScore = culpritMaxScore;
        this.evidenceMaxScore = evidenceMaxScore;
        this.methodMaxScore = methodMaxScore;
        this.motiveMaxScore = motiveMaxScore;
        this.coverUpMaxScore = coverUpMaxScore;
    }

    public int culpritMaxScore() {
        return culpritMaxScore;
    }

    public int evidenceMaxScore() {
        return evidenceMaxScore;
    }

    public int methodMaxScore() {
        return methodMaxScore;
    }

    public int motiveMaxScore() {
        return motiveMaxScore;
    }

    public int coverUpMaxScore() {
        return coverUpMaxScore;
    }
}
