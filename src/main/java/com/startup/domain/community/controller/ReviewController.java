package com.startup.domain.community.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.dto.ApiResponse;
import com.startup.domain.community.dto.ReviewCreateRequest;
import com.startup.domain.community.service.ReviewService;
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
@RequestMapping("/api/scenarios/{scenarioId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final MockUserProvider mockUserProvider;

    @Operation(summary = "시나리오 리뷰 작성")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addReview(
            @PathVariable Long scenarioId,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        Long userId = mockUserProvider.currentUserId();
        reviewService.addReview(userId, scenarioId, request);
        return ResponseEntity.ok(ApiResponse.empty());
    }
}
