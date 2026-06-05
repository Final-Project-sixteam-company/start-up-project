package com.startup.domain.scenario.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.dto.ApiResponse;
import com.startup.common.dto.PageResponse;
import com.startup.domain.scenario.dto.*;
import com.startup.domain.scenario.service.ScenarioService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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

    @Operation(summary = "커스텀 시나리오 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<ScenarioCreateResponse>> createScenario(
            @RequestBody ScenarioCreateRequest request
    ) {
        Long userId = mockUserProvider.currentUserId();
        ScenarioCreateResponse response = scenarioService.createScenario(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @Operation(summary = "시나리오 기본 정보 수정")
    @PatchMapping("/{scenarioId}")
    public ResponseEntity<ApiResponse<ScenarioUpdateResponse>> updateScenario(
            @PathVariable Long scenarioId,
            @RequestBody ScenarioUpdateRequest request
    ) {
        Long userId = mockUserProvider.currentUserId();
        ScenarioUpdateResponse response = scenarioService.updateScenario(userId, scenarioId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "시나리오 공개 등록(발행)")
    @PostMapping("/{scenarioId}/publish")
    public ResponseEntity<ApiResponse<ScenarioPublishResponse>> publishScenario(
            @PathVariable Long scenarioId,
            @RequestBody ScenarioPublishRequest request
    ) {
        Long userId = mockUserProvider.currentUserId();
        ScenarioPublishResponse response = scenarioService.publishScenario(userId, scenarioId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
