package com.startup.domain.community.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.dto.ApiResponse;
import com.startup.common.dto.PageResponse;
import com.startup.domain.community.dto.ReviewCreateRequest;
import com.startup.domain.community.dto.ReviewResponse;
import com.startup.domain.community.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "시나리오 리뷰 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getReviews(
            @PathVariable Long scenarioId,
            @RequestParam(required = false, defaultValue = "true") boolean includeSpoiler,
            Pageable pageable
    ) {
        PageResponse<ReviewResponse> response = reviewService.getReviews(scenarioId, includeSpoiler, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
