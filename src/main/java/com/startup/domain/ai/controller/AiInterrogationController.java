package com.startup.domain.ai.controller;

import com.startup.common.dto.ApiResponse;
import com.startup.domain.ai.dto.InterrogationRequest;
import com.startup.domain.ai.dto.InterrogationResponse;
import com.startup.domain.ai.service.AiInterrogationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Interrogation", description = "AI 용의자 심문 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/play-sessions/{sessionId}/interrogations")
public class AiInterrogationController {

    private final AiInterrogationService interrogationService;

    @Operation(summary = "AI 용의자 심문", description = "용의자에게 질문하고 AI 답변을 받는다.")
    @PostMapping
    public ResponseEntity<ApiResponse<InterrogationResponse>> interrogate(
            @PathVariable Long sessionId,
            @Valid @RequestBody InterrogationRequest request
    ) {
        InterrogationResponse response = interrogationService.interrogate(sessionId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
