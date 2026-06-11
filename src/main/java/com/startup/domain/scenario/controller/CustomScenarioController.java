package com.startup.domain.scenario.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.dto.ApiResponse;
import com.startup.domain.scenario.dto.*;
import com.startup.domain.scenario.service.CustomScenarioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @Operation(summary = "커스텀 시나리오 장소 목록 조회")
    @GetMapping("/{scenarioId}/locations")
    public ResponseEntity<ApiResponse<List<CustomLocationResponse>>> getLocations(
            @PathVariable Long scenarioId
    ) {
        Long userId = mockUserProvider.currentUserId();
        List<CustomLocationResponse> response = customScenarioService.getLocations(userId, scenarioId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "커스텀 시나리오 피해자 등록 및 수정(UPSERT)")
    @PostMapping("/{scenarioId}/victim")
    public ResponseEntity<ApiResponse<CustomVictimCreateResponse>> createOrUpdateVictim(
            @PathVariable Long scenarioId,
            @Valid @RequestBody CustomVictimCreateRequest request
    ) {
        Long userId = mockUserProvider.currentUserId();
        CustomVictimCreateResponse response = customScenarioService.createOrUpdateVictim(userId, scenarioId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "커스텀 시나리오 피해자 정보 조회")
    @GetMapping("/{scenarioId}/victim")
    public ResponseEntity<ApiResponse<CustomVictimResponse>> getVictim(
            @PathVariable Long scenarioId
    ) {
        Long userId = mockUserProvider.currentUserId();
        CustomVictimResponse response = customScenarioService.getVictim(userId, scenarioId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "커스텀 시나리오 용의자 목록 조회")
    @GetMapping("/{scenarioId}/suspects")
    public ResponseEntity<ApiResponse<List<CustomSuspectResponse>>> getSuspects(
            @PathVariable Long scenarioId
    ) {
        Long userId = mockUserProvider.currentUserId();
        List<CustomSuspectResponse> response = customScenarioService.getSuspects(userId, scenarioId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "커스텀 시나리오 용의자 등록")
    @PostMapping("/{scenarioId}/suspects")
    public ResponseEntity<ApiResponse<CustomSuspectCreateResponse>> createSuspect(
            @PathVariable Long scenarioId,
            @Valid @RequestBody CustomSuspectCreateRequest request
    ) {
        Long userId = mockUserProvider.currentUserId();
        CustomSuspectCreateResponse response = customScenarioService.createSuspect(userId, scenarioId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "커스텀 시나리오 증거 목록 조회")
    @GetMapping("/{scenarioId}/evidences")
    public ResponseEntity<ApiResponse<List<CustomEvidenceResponse>>> getEvidences(
            @PathVariable Long scenarioId
    ) {
        Long userId = mockUserProvider.currentUserId();
        List<CustomEvidenceResponse> response = customScenarioService.getEvidences(userId, scenarioId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "커스텀 시나리오 증거 등록 (관련 용의자 매핑 포함)")
    @PostMapping("/{scenarioId}/evidences")
    public ResponseEntity<ApiResponse<CustomEvidenceCreateResponse>> createEvidence(
            @PathVariable Long scenarioId,
            @Valid @RequestBody CustomEvidenceCreateRequest request
    ) {
        Long userId = mockUserProvider.currentUserId();
        CustomEvidenceCreateResponse response = customScenarioService.createEvidence(userId, scenarioId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "커스텀 시나리오 힌트 목록 조회")
    @GetMapping("/{scenarioId}/hints")
    public ResponseEntity<ApiResponse<List<CustomHintResponse>>> getHints(
            @PathVariable Long scenarioId
    ) {
        Long userId = mockUserProvider.currentUserId();
        List<CustomHintResponse> response = customScenarioService.getHints(userId, scenarioId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "커스텀 시나리오 힌트 등록")
    @PostMapping("/{scenarioId}/hints")
    public ResponseEntity<ApiResponse<CustomHintCreateResponse>> createHint(
            @PathVariable Long scenarioId,
            @Valid @RequestBody CustomHintCreateRequest request
    ) {
        Long userId = mockUserProvider.currentUserId();
        CustomHintCreateResponse response = customScenarioService.createHint(userId, scenarioId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "커스텀 시나리오 정답 등록 및 수정(UPSERT)")
    @PostMapping("/{scenarioId}/solution")
    public ResponseEntity<ApiResponse<CustomSolutionCreateResponse>> createOrUpdateSolution(
            @PathVariable Long scenarioId,
            @Valid @RequestBody CustomSolutionCreateRequest request
    ) {
        Long userId = mockUserProvider.currentUserId();
        CustomSolutionCreateResponse response = customScenarioService.createOrUpdateSolution(userId, scenarioId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "커스텀 시나리오 정답 조회 (작성자/관리자 전용)")
    @GetMapping("/{scenarioId}/solution")
    public ResponseEntity<ApiResponse<CustomSolutionResponse>> getSolution(
            @PathVariable Long scenarioId
    ) {
        Long userId = mockUserProvider.currentUserId();
        CustomSolutionResponse response = customScenarioService.getSolution(userId, scenarioId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
