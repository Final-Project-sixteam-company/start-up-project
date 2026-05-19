package com.startup.domain.scenario.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.dto.ApiResponse;
import com.startup.common.dto.PageResponse;
import com.startup.domain.scenario.dto.ScenarioDetailResponse;
import com.startup.domain.scenario.dto.ScenarioSearchCondition;
import com.startup.domain.scenario.dto.ScenarioSummaryResponse;
import com.startup.domain.scenario.service.ScenarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scenarios")
@RequiredArgsConstructor
public class ScenarioController {

    private final ScenarioService scenarioService;
    private final MockUserProvider mockUserProvider;

    @GetMapping
    public ApiResponse<PageResponse<ScenarioSummaryResponse>> getScenarios(
            ScenarioSearchCondition condition,
            Pageable pageable
    ) {
        Long userId = mockUserProvider.currentUserId();
        PageResponse<ScenarioSummaryResponse> response = scenarioService.getScenarios(userId, condition, pageable);
        return ApiResponse.success(response);
    }

    @GetMapping("/{scenarioId}")
    public ApiResponse<ScenarioDetailResponse> getScenario(
            @PathVariable Long scenarioId
    ) {
        Long userId = mockUserProvider.currentUserId();
        ScenarioDetailResponse response = scenarioService.getScenario(userId, scenarioId);
        return ApiResponse.success(response);
    }
}
