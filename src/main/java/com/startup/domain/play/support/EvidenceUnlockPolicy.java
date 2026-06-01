package com.startup.domain.play.support;

import com.startup.domain.scenario.entity.Evidence;
import com.startup.domain.scenario.entity.EvidenceUnlockRule;
import com.startup.domain.scenario.enums.EvidenceUnlockType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class EvidenceUnlockPolicy {

    private static final int PHASE_INTERVAL_MINUTES = 5;
    private static final TypeReference<Map<String, Object>> CONDITION_TYPE = new TypeReference<>() {};

    private final JsonMapper jsonMapper;

    public boolean canAutoUnlock(Evidence evidence, int elapsedMinutes) {
        return canAutoUnlock(evidence, null, elapsedMinutes, Set.of());
    }

    public boolean canAutoUnlock(Evidence evidence,
                                 EvidenceUnlockRule unlockRule,
                                 int elapsedMinutes,
                                 Set<String> unlockedEvidenceCodes) {
        if (evidence == null || Boolean.TRUE.equals(evidence.getIsInitialPublic())) {
            return false;
        }
        boolean timingSatisfied = false;
        if (EvidenceUnlockType.TIME == evidence.getUnlockType()) {
            timingSatisfied = evidence.getUnlockAfterMinutes() != null
                    && elapsedMinutes >= evidence.getUnlockAfterMinutes();
        }
        if (EvidenceUnlockType.PHASE == evidence.getUnlockType()) {
            Integer requiredMinutes = requiredElapsedMinutesForPhase(firstNonBlank(
                    evidence.getUnlockPhase(),
                    unlockRule == null ? null : unlockRule.getRequiredPhase()
            ));
            timingSatisfied = requiredMinutes != null && elapsedMinutes >= requiredMinutes;
        }
        return timingSatisfied && prerequisiteEvidenceSatisfied(unlockRule, unlockedEvidenceCodes);
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

    private boolean prerequisiteEvidenceSatisfied(EvidenceUnlockRule unlockRule, Set<String> unlockedEvidenceCodes) {
        List<String> requiredEvidenceCodes = requiredEvidenceCodes(unlockRule);
        if (requiredEvidenceCodes.isEmpty()) {
            return true;
        }
        return unlockedEvidenceCodes != null && unlockedEvidenceCodes.containsAll(requiredEvidenceCodes);
    }

    private List<String> requiredEvidenceCodes(EvidenceUnlockRule unlockRule) {
        if (unlockRule == null || unlockRule.getConditionJson() == null || unlockRule.getConditionJson().isBlank()) {
            return List.of();
        }
        try {
            Map<String, Object> condition = jsonMapper.readValue(unlockRule.getConditionJson(), CONDITION_TYPE);
            Object value = condition.get("requiredEvidenceCodes");
            if (value instanceof Collection<?> collection) {
                return collection.stream()
                        .map(String::valueOf)
                        .filter(item -> !item.isBlank())
                        .toList();
            }
            if (value instanceof String stringValue && !stringValue.isBlank()) {
                return List.of(stringValue);
            }
            return List.of();
        } catch (Exception e) {
            log.warn("unlockRule conditionJson 파싱 실패. evidenceId={}, conditionJson={}",
                    unlockRule.getEvidenceId(), unlockRule.getConditionJson(), e);
            return List.of("__INVALID_UNLOCK_CONDITION__");
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
