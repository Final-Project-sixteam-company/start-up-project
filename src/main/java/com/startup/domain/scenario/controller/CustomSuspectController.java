package com.startup.domain.scenario.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.dto.ApiResponse;
import com.startup.domain.scenario.dto.CustomSuspectResponse;
import com.startup.domain.scenario.dto.CustomSuspectUpdateRequest;
import com.startup.domain.scenario.service.CustomScenarioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suspects")
@RequiredArgsConstructor
public class CustomSuspectController {

    private final CustomScenarioService customScenarioService;
    private final MockUserProvider mockUserProvider;

    @Operation(summary = "커스텀 시나리오 용의자 수정")
    @PatchMapping("/{suspectId}")
    public ResponseEntity<ApiResponse<CustomSuspectResponse>> updateSuspect(
            @PathVariable Long suspectId,
            @Valid @RequestBody CustomSuspectUpdateRequest request
    ) {
        Long userId = mockUserProvider.currentUserId();
        CustomSuspectResponse response = customScenarioService.updateSuspect(userId, suspectId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "커스텀 시나리오 용의자 삭제")
    @DeleteMapping("/{suspectId}")
    public ResponseEntity<ApiResponse<Void>> deleteSuspect(@PathVariable Long suspectId) {
        Long userId = mockUserProvider.currentUserId();
        customScenarioService.deleteSuspect(userId, suspectId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
