package com.startup.domain.scenario.service;

import com.startup.domain.scenario.error.ScenarioErrorCode;
import com.startup.domain.scenario.error.ScenarioException;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.repository.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
// 초기 MVP에서는 모든 접근을 허용한다.
// 인증/거래 도입 후 무료/유료/작성자/구매 여부 검증을 이 서비스 안으로 모은다.
public class ScenarioAccessService {

    private final ScenarioRepository scenarioRepository;

    public boolean canPlay(Long userId, Long scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId).orElse(null);

        if (scenario == null) return false;

        // 작성자 본인이거나, PUBLISHED 상태인 경우에만 플레이 가능
        boolean isCreator = userId != null && userId.equals(scenario.getCreatorId());
        return isCreator || scenario.getStatus() == ScenarioStatus.PUBLISHED;
    }

    public boolean canEdit(Long userId, Long scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId).orElse(null);
        if (scenario == null) return false;
        
        return userId != null && userId.equals(scenario.getCreatorId());
    }

    public boolean canView(Long userId, Long scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId).orElse(null);
        if (scenario == null) return false;
        
        if (scenario.getStatus() == ScenarioStatus.PUBLISHED) return true;
        
        return userId != null && userId.equals(scenario.getCreatorId());
    }

    public void validatePlayable(Long userId, Long scenarioId) {
        if (!canPlay(userId, scenarioId)) {
            throw new ScenarioException(ScenarioErrorCode.SCENARIO_ACCESS_DENIED);
        }
    }

    public void validateEditable(Long userId, Long scenarioId) {
        if (!canEdit(userId, scenarioId)) {
            throw new ScenarioException(ScenarioErrorCode.SCENARIO_ACCESS_DENIED);
        }
    }

    public void validateViewable(Long userId, Long scenarioId) {
        if (!canView(userId, scenarioId)) {
            throw new ScenarioException(ScenarioErrorCode.SCENARIO_ACCESS_DENIED);
        }
    }
}
