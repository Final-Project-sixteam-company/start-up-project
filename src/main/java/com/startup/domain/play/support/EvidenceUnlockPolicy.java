package com.startup.domain.play.support;

import com.startup.domain.scenario.entity.Evidence;
import com.startup.domain.scenario.enums.EvidenceUnlockType;
import org.springframework.stereotype.Component;

@Component
public class EvidenceUnlockPolicy {

    private static final int PHASE_INTERVAL_MINUTES = 5;

    public boolean canAutoUnlock(Evidence evidence, int elapsedMinutes) {
        if (evidence == null || Boolean.TRUE.equals(evidence.getIsInitialPublic())) {
            return false;
        }
        if (EvidenceUnlockType.TIME == evidence.getUnlockType()) {
            return evidence.getUnlockAfterMinutes() != null
                    && elapsedMinutes >= evidence.getUnlockAfterMinutes();
        }
        if (EvidenceUnlockType.PHASE == evidence.getUnlockType()) {
            Integer requiredMinutes = requiredElapsedMinutesForPhase(evidence.getUnlockPhase());
            return requiredMinutes != null && elapsedMinutes >= requiredMinutes;
        }
        return false;
    }

    public String buildUnlockHint(Evidence evidence) {
        if (EvidenceUnlockType.TIME == evidence.getUnlockType()
                && evidence.getUnlockAfterMinutes() != null) {
            return evidence.getUnlockAfterMinutes() + "분 후 공개";
        }
        if (EvidenceUnlockType.PHASE == evidence.getUnlockType()) {
            Integer requiredMinutes = requiredElapsedMinutesForPhase(evidence.getUnlockPhase());
            if (requiredMinutes == null) {
                return "조사 단계 진행 시 공개";
            }
            if (requiredMinutes <= 0) {
                return "조사 시작 후 공개";
            }
            return requiredMinutes + "분 후 공개";
        }
        if (EvidenceUnlockType.INTERROGATION == evidence.getUnlockType()) {
            return "심문을 통해 해금";
        }
        if (EvidenceUnlockType.EVIDENCE_PRESENTED == evidence.getUnlockType()) {
            return "증거 제시로 해금";
        }
        return "조건 충족 시 해금";
    }

    private Integer requiredElapsedMinutesForPhase(String unlockPhase) {
        Integer phaseNumber = parsePhaseNumber(unlockPhase);
        if (phaseNumber == null) {
            return null;
        }
        if (phaseNumber <= 1) {
            return 0;
        }
        return (phaseNumber - 1) * PHASE_INTERVAL_MINUTES;
    }

    private Integer parsePhaseNumber(String unlockPhase) {
        if (unlockPhase == null || !unlockPhase.startsWith("PHASE_")) {
            return null;
        }
        int start = "PHASE_".length();
        int end = unlockPhase.indexOf('_', start);
        String number = end >= 0 ? unlockPhase.substring(start, end) : unlockPhase.substring(start);
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
