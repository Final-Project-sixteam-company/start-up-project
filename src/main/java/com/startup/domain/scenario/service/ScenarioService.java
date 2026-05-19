package com.startup.domain.scenario.service;

import com.startup.common.dto.PageResponse;
import com.startup.common.error.BusinessException;
import com.startup.common.error.CommonErrorCode;
import com.startup.domain.scenario.dto.ScenarioDetailResponse;
import com.startup.domain.scenario.dto.ScenarioSearchCondition;
import com.startup.domain.scenario.dto.ScenarioSummaryResponse;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.repository.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioAccessService scenarioAccessService;

    @Transactional(readOnly = true)
    public PageResponse<ScenarioSummaryResponse> getScenarios(Long userId, ScenarioSearchCondition condition, Pageable pageable) {
        // TODO: 검색 조건(condition)에 맞춰 QueryDSL이나 Specification을 사용한 세부 필터링 구현하기
        Page<Scenario> scenarios = scenarioRepository.findAll(pageable);
        
        Page<ScenarioSummaryResponse> responsePage = scenarios.map(scenario -> 
            ScenarioSummaryResponse.from(scenario, false) // TODO: 실제 북마크 여부 확인 로직 추가하기
        );
        
        return PageResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public ScenarioDetailResponse getScenario(Long userId, Long scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.SCENARIO_NOT_FOUND));

        scenarioAccessService.validateViewable(userId, scenarioId);

        // TODO: 실제 작성자 닉네임 조회 및 북마크 여부 확인 로직 구현하기
        String mockCreatorNickname = "운영자"; 
        Boolean isBookmarked = false;
        Boolean canPlay = scenarioAccessService.canPlay(userId, scenarioId);

        return ScenarioDetailResponse.from(scenario, mockCreatorNickname, isBookmarked, canPlay);
    }
}
