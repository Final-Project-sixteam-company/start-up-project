package com.startup.domain.community.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.dto.ApiResponse;
import com.startup.domain.community.dto.ReportCreateRequest;
import com.startup.domain.community.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scenarios/{scenarioId}/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final MockUserProvider mockUserProvider;

    @Operation(summary = "시나리오 신고")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addReport(
            @PathVariable Long scenarioId,
            @Valid @RequestBody ReportCreateRequest request
    ) {
        Long reporterId = mockUserProvider.currentUserId();
        reportService.addReport(reporterId, scenarioId, request);
        return ResponseEntity.ok(ApiResponse.empty());
    }
}
