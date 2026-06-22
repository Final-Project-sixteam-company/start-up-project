package com.startup.domain.ai.controller;

import com.startup.common.dto.ApiResponse;
import com.startup.domain.ai.dto.DeductionResultResponse;
import com.startup.domain.ai.dto.FinalDeductionRequest;
import com.startup.domain.ai.dto.FinalDeductionResponse;
import com.startup.domain.ai.service.AiDeductionScorer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Deduction", description = "최종 추리 채점 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/play-sessions/{sessionId}")
public class AiDeductionController {

    private final AiDeductionScorer aiDeductionScorer;

    @Operation(summary = "최종 추리 제출", description = "최종 추리를 제출하고 채점 결과를 받는다.")
    @PostMapping("/final-deduction")
    public ResponseEntity<ApiResponse<FinalDeductionResponse>> submitFinalDeduction(
            @PathVariable Long sessionId,
            @Valid @RequestBody FinalDeductionRequest request) {
        FinalDeductionResponse response = aiDeductionScorer.submitAndScore(sessionId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "채점 결과 조회", description = "채점 결과와 해설을 조회한다.")
    @GetMapping("/result")
    public ResponseEntity<ApiResponse<DeductionResultResponse>> getResult(
            @PathVariable Long sessionId) {
        DeductionResultResponse response = aiDeductionScorer.getResult(sessionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
