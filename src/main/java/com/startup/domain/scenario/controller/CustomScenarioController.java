package com.startup.domain.scenario.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.dto.ApiResponse;
import com.startup.domain.scenario.dto.CustomLocationCreateRequest;
import com.startup.domain.scenario.dto.CustomLocationCreateResponse;
import com.startup.domain.scenario.service.CustomScenarioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scenarios")
@RequiredArgsConstructor
public class CustomScenarioController {

    private final CustomScenarioService customScenarioService;
    private final MockUserProvider mockUserProvider;

    @Operation(summary = "커스텀 시나리오 장소 등록")
    @PostMapping("/{scenarioId}/locations")
    public ResponseEntity<ApiResponse<CustomLocationCreateResponse>> createLocation(
            @PathVariable Long scenarioId,
            @Valid @RequestBody CustomLocationCreateRequest request
    ) {
        Long userId = mockUserProvider.currentUserId();
        CustomLocationCreateResponse response = customScenarioService.createLocation(userId, scenarioId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
