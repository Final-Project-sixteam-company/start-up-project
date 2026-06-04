package com.startup.domain.play.service;

import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.entity.UnlockedEvidence;
import com.startup.domain.play.repository.PlaySessionRepository;
import com.startup.domain.play.repository.UnlockedEvidenceRepository;
import com.startup.domain.scenario.entity.Evidence;
import com.startup.domain.scenario.entity.EvidenceUnlockRule;
import com.startup.domain.scenario.entity.Suspect;
import com.startup.domain.scenario.enums.EvidenceUnlockType;
import com.startup.domain.scenario.repository.EvidenceRepository;
import com.startup.domain.scenario.repository.EvidenceUnlockRuleRepository;
import com.startup.domain.scenario.repository.SuspectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 심문 중 증거 제시(EVIDENCE_PRESENTED)로 새 증거를 해금한다.
 * <p>
 * ADR-3: 해금 write-path와 unlocked_evidences(SoT)는 domain/play가 소유한다.
 * domain/ai(AiInterrogationService)는 {@link TimeEvidenceUnlockSyncer}와 동일하게 이 서비스를
 * 동기 호출하고, 새로 해금된 증거 diff를 반환받아 심문 응답에 즉시 반영한다(ai→play 단방향).
 * <p>
 * 규칙은 DB 스키마 변경 없이 기존 evidence_unlock_rules.condition_json으로 표현한다:
 * unlock_type=EVIDENCE_PRESENTED, condition.requiredPresentedEvidenceCode(제시 증거),
 * condition.requiredCharacterCode(제시 대상 용의자), condition.requiredEvidenceCodes(선행 해금 조건).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterrogationEvidenceUnlockService {

    private static final TypeReference<Map<String, Object>> CONDITION_TYPE = new TypeReference<>() {};

    private final PlaySessionRepository playSessionRepository;
    private final EvidenceRepository evidenceRepository;
    private final SuspectRepository suspectRepository;
    private final EvidenceUnlockRuleRepository evidenceUnlockRuleRepository;
    private final UnlockedEvidenceRepository unlockedEvidenceRepository;
    private final JsonMapper jsonMapper;

    /** 새로 해금된 증거 요약. */
    public record UnlockedEvidenceResult(Long evidenceId, String title) {}

    /**
     * 제시된 증거를 트리거로 매칭되는 EVIDENCE_PRESENTED unlock 규칙을 평가해 새 증거를 해금한다.
     * 멱등: 이미 해금된 증거는 다시 해금되지 않으며(insert ignore), 그 경우 diff에 포함되지 않는다.
     * 호출 측(AiInterrogationService)에서 세션 소유자/PLAYING 검증을 이미 마친 뒤 호출된다.
     *
     * @return 이번 호출로 새로 해금된 증거 목록(없으면 빈 리스트)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<UnlockedEvidenceResult> unlockByPresentedEvidence(
            Long sessionId, Long suspectId, Long presentedEvidenceId, Long currentUserId) {
        if (presentedEvidenceId == null || suspectId == null) {
            return List.of();
        }
        PlaySession session = playSessionRepository.findById(sessionId).orElse(null);
        if (session == null || !session.isPlaying()) {
            return List.of();
        }
        // 호출부에서 이미 소유자/PLAYING을 검증하지만, write-path가 스스로 보호하도록 소유자 이중 확인한다.
        if (currentUserId != null && !currentUserId.equals(session.getUserId())) {
            return List.of();
        }
        Long scenarioId = session.getScenarioId();

        Evidence presented = evidenceRepository.findById(presentedEvidenceId).orElse(null);
        Suspect suspect = suspectRepository.findById(suspectId).orElse(null);
        if (presented == null || suspect == null
                || presented.getCode() == null || suspect.getCode() == null
                || !scenarioId.equals(presented.getScenarioId())
                || !scenarioId.equals(suspect.getScenarioId())) {
            // 교차 시나리오 오염 방어: 제시 증거·용의자는 현재 세션 시나리오 소속이어야 한다.
            return List.of();
        }
        String presentedCode = presented.getCode();
        String suspectCode = suspect.getCode();

        Set<Long> alreadyUnlockedIds = unlockedEvidenceRepository.findAllByPlaySessionId(sessionId)
                .stream()
                .map(UnlockedEvidence::getEvidenceId)
                .collect(Collectors.toSet());
        Set<String> alreadyUnlockedCodes = evidenceRepository.findAllByScenarioIdOrderBySortOrder(scenarioId)
                .stream()
                .filter(e -> alreadyUnlockedIds.contains(e.getId()))
                .map(Evidence::getCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<UnlockedEvidenceResult> newlyUnlocked = new ArrayList<>();
        for (EvidenceUnlockRule rule : evidenceUnlockRuleRepository.findAllByScenarioIdOrderBySortOrder(scenarioId)) {
            if (!EvidenceUnlockType.EVIDENCE_PRESENTED.name().equals(rule.getUnlockType())) {
                continue;
            }
            if (alreadyUnlockedIds.contains(rule.getEvidenceId())) {
                continue;
            }
            if (!matches(rule, presentedCode, suspectCode, alreadyUnlockedCodes)) {
                continue;
            }
            // 교차 시나리오 오염 방어: 해금 대상 증거도 현재 시나리오 소속이어야 한다.
            Evidence target = evidenceRepository.findById(rule.getEvidenceId()).orElse(null);
            if (target == null || !scenarioId.equals(target.getScenarioId())) {
                continue;
            }
            int inserted = unlockedEvidenceRepository.insertIgnoreUnlockedEvidence(
                    sessionId, target.getId(), "EVIDENCE_PRESENTED");
            if (inserted > 0) {
                newlyUnlocked.add(new UnlockedEvidenceResult(target.getId(), target.getTitle()));
            }
        }
        return newlyUnlocked;
    }

    private boolean matches(EvidenceUnlockRule rule, String presentedCode, String suspectCode,
                            Set<String> alreadyUnlockedCodes) {
        Map<String, Object> condition = parseCondition(rule);
        String requiredPresented = asString(condition.get("requiredPresentedEvidenceCode"));
        // 제시 증거 코드가 일치해야 한다(필수).
        if (requiredPresented == null || !requiredPresented.equals(presentedCode)) {
            return false;
        }
        // 제시 대상 용의자 코드가 지정돼 있으면 일치해야 한다.
        String requiredCharacter = asString(condition.get("requiredCharacterCode"));
        if (requiredCharacter != null && !requiredCharacter.equals(suspectCode)) {
            return false;
        }
        // 선행 해금 증거 조건이 있으면 모두 해금돼 있어야 한다.
        List<String> prerequisites = asStringList(condition.get("requiredEvidenceCodes"));
        return prerequisites.isEmpty() || alreadyUnlockedCodes.containsAll(prerequisites);
    }

    private Map<String, Object> parseCondition(EvidenceUnlockRule rule) {
        if (rule.getConditionJson() == null || rule.getConditionJson().isBlank()) {
            return Map.of();
        }
        try {
            return jsonMapper.readValue(rule.getConditionJson(), CONDITION_TYPE);
        } catch (Exception e) {
            // 민감 정보 노출 방지: conditionJson 전문·스택트레이스 대신 evidenceId와 예외 클래스만 남긴다.
            log.warn("EVIDENCE_PRESENTED unlock conditionJson 파싱 실패. evidenceId={}, error={}",
                    rule.getEvidenceId(), e.getClass().getSimpleName());
            return Map.of();
        }
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() || "null".equals(text) ? null : text;
    }

    private List<String> asStringList(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(String::valueOf)
                    .filter(item -> !item.isBlank())
                    .toList();
        }
        return List.of();
    }
}
