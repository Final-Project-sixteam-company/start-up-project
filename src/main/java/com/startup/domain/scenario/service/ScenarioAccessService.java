package com.startup.domain.scenario.service;

import com.startup.domain.play.enums.PlaySessionStatus;
import com.startup.domain.play.repository.PlaySessionRepository;
import com.startup.domain.scenario.error.ScenarioErrorCode;
import com.startup.domain.scenario.error.ScenarioException;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioVisibility;
import com.startup.domain.scenario.repository.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
// 인증/거래 도입 후 무료/유료/작성자/구매 여부 검증을 이 서비스 안으로 모은다.
public class ScenarioAccessService {

    private final ScenarioRepository scenarioRepository;
    private final PlaySessionRepository playSessionRepository;

    public boolean canPlay(Long userId, Long scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId).orElse(null);

        if (scenario == null) return false;

        if (scenario.getStatus() == ScenarioStatus.DELETED) {
            // 삭제된 시나리오라도, 이미 플레이 중인 세션이 있다면 계속 진행 가능하도록 보장
            return userId != null && playSessionRepository.existsByUserIdAndScenarioIdAndStatus(userId, scenarioId, PlaySessionStatus.PLAYING);
        }

        boolean isCreator = userId != null && userId.equals(scenario.getCreatorId());
        if (isCreator || isPubliclyAccessible(scenario)) {
            return true;
        }

        // HIDDEN 상태라도 기존 플레이 세션이 있다면(PLAYING) 계속 플레이(접근) 가능하도록 보장
        return userId != null && playSessionRepository.existsByUserIdAndScenarioIdAndStatus(userId, scenarioId, PlaySessionStatus.PLAYING);
    }

    public boolean canEditDraftLike(Long userId, Long scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId).orElse(null);
        if (scenario == null || scenario.getStatus() == ScenarioStatus.DELETED) return false;
        
        return userId != null && userId.equals(scenario.getCreatorId());
    }

    public boolean canView(Long userId, Long scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId).orElse(null);
        if (scenario == null) return false;

        if (scenario.getStatus() == ScenarioStatus.DELETED) {
            // 삭제된 시나리오라도, 이미 플레이 중인 세션이 있다면 계속 볼 수 있도록 보장
            return userId != null && playSessionRepository.existsByUserIdAndScenarioIdAndStatus(userId, scenarioId, PlaySessionStatus.PLAYING);
        }
        
        boolean isCreator = userId != null && userId.equals(scenario.getCreatorId());
        if (isCreator || isPubliclyAccessible(scenario)) {
            return true;
        }

        // HIDDEN 상태라도 기존 플레이 세션이 있다면(PLAYING) 계속 볼 수 있도록 보장
        return userId != null && playSessionRepository.existsByUserIdAndScenarioIdAndStatus(userId, scenarioId, PlaySessionStatus.PLAYING);
    }

    public void validatePlayable(Long userId, Long scenarioId) {
        if (!canPlay(userId, scenarioId)) {
            throw new ScenarioException(ScenarioErrorCode.SCENARIO_ACCESS_DENIED);
        }
    }

    public void validateDraftEditable(Long userId, Long scenarioId) {
        if (!canEditDraftLike(userId, scenarioId)) {
            throw new ScenarioException(ScenarioErrorCode.SCENARIO_ACCESS_DENIED);
        }
    }

    public void validateViewable(Long userId, Long scenarioId) {
        if (!canView(userId, scenarioId)) {
            throw new ScenarioException(ScenarioErrorCode.SCENARIO_ACCESS_DENIED);
        }
    }

    private boolean isPubliclyAccessible(Scenario scenario) {
        if (scenario.getStatus() != ScenarioStatus.PUBLISHED) {
            return false;
        }
        return scenario.getVisibility() == ScenarioVisibility.PUBLIC
                || scenario.getVisibility() == ScenarioVisibility.OFFICIAL
                || scenario.getVisibility() == ScenarioVisibility.UNLISTED;
    }
}
