package com.startup.domain.ai.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.dto.ApiResponse;
import com.startup.domain.ai.dto.ScenarioValidationResponse;
import com.startup.domain.ai.service.AiScenarioValidationService;
import com.startup.domain.scenario.service.ScenarioAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Scenario Validation", description = "AI 시나리오 검증 API")
@RestController
@RequiredArgsConstructor
public class AiScenarioValidationController {

    private final AiScenarioValidationService validationService;
    private final MockUserProvider mockUserProvider;
    private final ScenarioAccessService scenarioAccessService;

    @Operation(summary = "시나리오 AI 검증", description = "시나리오의 논리적 완결성을 검증한다.")
    @PostMapping("/api/ai/scenarios/{scenarioId}/validate")
    public ResponseEntity<ApiResponse<ScenarioValidationResponse>> validate(
            @PathVariable Long scenarioId
    ) {
        Long userId = mockUserProvider.currentUserId();
        scenarioAccessService.validateDraftEditable(userId, scenarioId);

        ScenarioValidationResponse response = validationService.validate(scenarioId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "검증 결과 조회", description = "최신 시나리오 검증 결과를 조회한다.")
    @GetMapping("/api/scenarios/{scenarioId}/validation-result")
    public ResponseEntity<ApiResponse<ScenarioValidationResponse>> getValidationResult(
            @PathVariable Long scenarioId
    ) {
        Long userId = mockUserProvider.currentUserId();
        scenarioAccessService.validateDraftEditable(userId, scenarioId);

        ScenarioValidationResponse response = validationService.getLatestResult(scenarioId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
