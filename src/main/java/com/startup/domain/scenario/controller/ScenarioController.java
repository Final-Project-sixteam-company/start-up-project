package com.startup.domain.scenario.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.dto.ApiResponse;
import com.startup.common.dto.PageResponse;
import com.startup.domain.scenario.dto.ScenarioDetailResponse;
import com.startup.domain.scenario.dto.ScenarioSearchCondition;
import com.startup.domain.scenario.dto.ScenarioSummaryResponse;
import com.startup.domain.scenario.service.ScenarioService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scenarios")
@RequiredArgsConstructor
public class ScenarioController {

    private final ScenarioService scenarioService;
    private final MockUserProvider mockUserProvider;

    @Operation(summary = "시나리오 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ScenarioSummaryResponse>>> getScenarios(
            ScenarioSearchCondition condition,
            Pageable pageable
    ) {
        Long userId = mockUserProvider.currentUserId();
        PageResponse<ScenarioSummaryResponse> response = scenarioService.getScenarios(userId, condition, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "시나리오 상세 조회")
    @GetMapping("/{scenarioId}")
    public ResponseEntity<ApiResponse<ScenarioDetailResponse>> getScenario(
            @PathVariable Long scenarioId
    ) {
        Long userId = mockUserProvider.currentUserId();
        ScenarioDetailResponse response = scenarioService.getScenario(userId, scenarioId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
