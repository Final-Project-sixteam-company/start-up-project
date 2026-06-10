package com.startup.domain.scenario.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.dto.ApiResponse;
import com.startup.domain.scenario.dto.CustomEvidenceResponse;
import com.startup.domain.scenario.dto.CustomEvidenceUpdateRequest;
import com.startup.domain.scenario.service.CustomScenarioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/evidences")
@RequiredArgsConstructor
public class CustomEvidenceController {

    private final CustomScenarioService customScenarioService;
    private final MockUserProvider mockUserProvider;

    @Operation(summary = "커스텀 시나리오 증거 수정")
    @PatchMapping("/{evidenceId}")
    public ResponseEntity<ApiResponse<CustomEvidenceResponse>> updateEvidence(
            @PathVariable Long evidenceId,
            @Valid @RequestBody CustomEvidenceUpdateRequest request
    ) {
        Long userId = mockUserProvider.currentUserId();
        CustomEvidenceResponse response = customScenarioService.updateEvidence(userId, evidenceId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "커스텀 시나리오 증거 삭제")
    @DeleteMapping("/{evidenceId}")
    public ResponseEntity<ApiResponse<Void>> deleteEvidence(@PathVariable Long evidenceId) {
        Long userId = mockUserProvider.currentUserId();
        customScenarioService.deleteEvidence(userId, evidenceId);
        return ResponseEntity.ok(ApiResponse.empty());
    }
}
